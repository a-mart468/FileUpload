package com.fileserver.fileupload.service;

import com.fileserver.fileupload.exception.FileValidationException;
import com.fileserver.fileupload.model.FileMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fileserver.fileupload.exception.StoredFileNotFoundException;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path uploadDirectory;
    private final long maxSizeBytes;
    private final Set<String> allowedTypes;
    private final Map<String,FileMetadata> files = new ConcurrentHashMap<>();

    public FileStorageService(
            @Value("${file.upload.directory}") String directory,
            @Value("${file.upload.max-size-bytes}") long maxSizeBytes,
            @Value("${file.upload.allowed-types}") String allowedTypes) {

        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();

        this.maxSizeBytes = maxSizeBytes;

        this.allowedTypes = Arrays.stream(allowedTypes.split(",")).map(String::trim).map(type -> type.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not create upload directory.",
                    exception
            );
        }
    }

    public FileMetadata store(MultipartFile file) {
        String filename = validate(file);
        String id = UUID.randomUUID().toString();
        Path targetPath = uploadDirectory.resolve(id);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not store file.",
                    exception
            );
        }

        FileMetadata metadata = new FileMetadata(
                id,
                filename,
                file.getSize(),
                file.getContentType(),
                Instant.now()
        );
        files.put(id, metadata);
        return metadata;
    }

    public List<FileMetadata> getAllFiles(){
        return files.values().stream().sorted(Comparator.comparing(FileMetadata::createdAt).reversed()).toList();

    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("EMPTY_FILE", "File must not be empty.", Map.of()
            );
        }

        if (file.getSize() > maxSizeBytes) {
            throw new FileValidationException(
                    "MAX_SIZE_EXCEEDED",
                    "File too large. Max " + maxSizeBytes + " bytes.",
                    Map.of("limitBytes", maxSizeBytes, "actualBytes", file.getSize())
            );
        }

        String contentType = file.getContentType();
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (!allowedTypes.contains(normalizedType)) {
            throw new FileValidationException("UNSUPPORTED_MEDIA_TYPE", "File type is not allowed.", Map.of("contentType", contentType == null ? "unknown" : contentType, "allowedTypes", allowedTypes));
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new FileValidationException("INVALID_FILENAME", "File name must not be empty.", Map.of());
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileValidationException(
                    "INVALID_FILENAME",
                    "File name contains an invalid path.",
                    Map.of("filename", filename)
            );
        }

        return filename;
    }

    public FileMetadata getFileById(String id) {
        FileMetadata metadata = files.get(id);

        if (metadata == null) {
            throw new StoredFileNotFoundException(id);
        }

        return metadata;
    }

    public Resource loadFile(String id) {
        getFileById(id);

        Path filePath = uploadDirectory.resolve(id).normalize();

        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {

            throw new StoredFileNotFoundException(id);
        }

        return new FileSystemResource(filePath);
    }
}
