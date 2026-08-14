package com.fileserver.fileupload.service;

import com.fileserver.fileupload.exception.FileValidationException;
import com.fileserver.fileupload.exception.StoredFileNotFoundException;
import com.fileserver.fileupload.entity.ActivityAction;
import com.fileserver.fileupload.entity.FileMetadata;
import com.fileserver.fileupload.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path uploadDirectory;
    private final long maxSizeBytes;
    private final Set<String> allowedTypes;
    private final FileMetadataRepository fileMetadataRepository;
    private final ActivityLogService activityLogService;

    public FileStorageService(@Value("${file.upload.directory}") String directory, @Value("${file.upload.max-size-bytes}") long maxSizeBytes, @Value("${file.upload.allowed-types}") String allowedTypes, FileMetadataRepository fileMetadataRepository, ActivityLogService activityLogService) {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();

        this.maxSizeBytes = maxSizeBytes;

        this.allowedTypes = Arrays.stream(allowedTypes.split(",")).map(String::trim).map(type -> type.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());

        this.fileMetadataRepository = fileMetadataRepository;
        this.activityLogService = activityLogService;

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not create upload directory.",
                    exception
            );
        }
    }

    @Transactional
    public FileMetadata store(MultipartFile file) {
        String filename = validate(file);

        FileMetadata metadata = storeValidated(file, filename);

        try {
            activityLogService.log(currentUsername(), ActivityAction.FILE_UPLOAD, true, "POST", "/api/files", metadata.getId(), "File uploaded: " + metadata.getFilename());
            return metadata;
        } catch (RuntimeException exception) {

            deletePhysicalFile(metadata.getId(), exception);

            throw exception;
        }
    }

    @Transactional
    public List<FileMetadata> storeAll(List<MultipartFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            throw new FileValidationException("EMPTY_BATCH", "At least one file is required.", Map.of());
        }
        List<ValidatedFile> validatedFiles = uploadedFiles.stream().map(file -> new ValidatedFile(file, validate(file))).toList();
        List<FileMetadata> storedFiles = new ArrayList<>();

        try {
            for (ValidatedFile validatedFile : validatedFiles) {
                FileMetadata metadata = storeValidated(validatedFile.file(), validatedFile.filename());

                storedFiles.add(metadata);
            }

            activityLogService.log(currentUsername(), ActivityAction.BATCH_UPLOAD, true, "POST", "/api/files/batch", null, "Uploaded " + storedFiles.size() + " files");

            return List.copyOf(storedFiles);

        } catch (RuntimeException exception) {

            rollbackPhysicalFiles(storedFiles, exception);

            throw exception;
        }
    }

    private FileMetadata storeValidated(MultipartFile file, String filename) {
        String id = UUID.randomUUID().toString();
        Path targetPath = uploadDirectory.resolve(id);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            deletePhysicalFile(id, exception);

            throw new IllegalStateException("Could not store file.", exception);
        }

        FileMetadata metadata = new FileMetadata(id, filename, file.getSize(), file.getContentType(), Instant.now());

        try {
            return fileMetadataRepository.save(metadata);

        } catch (RuntimeException exception) {

            deletePhysicalFile(id, exception);
            throw exception;
        }
    }

    @Transactional
    public List<FileMetadata> getAllFiles() {
        List<FileMetadata> files = fileMetadataRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        activityLogService.log(currentUsername(), ActivityAction.FILE_LIST, true, "GET", "/api/files", null, "Retrieved list containing " + files.size() + " files");

        return files;
    }

    @Transactional(readOnly = true)
    public FileMetadata getFileById(String id) {
        return findMetadataById(id);
    }

    @Transactional
    public Resource loadFile(String id) {
        FileMetadata metadata = findMetadataById(id);

        Path filePath = uploadDirectory.resolve(id).normalize();

        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {

            throw new StoredFileNotFoundException(id);
        }

        Resource resource = new FileSystemResource(filePath);

        activityLogService.log(currentUsername(), ActivityAction.FILE_DOWNLOAD, true, "GET", "/api/files/" + id, id, "File downloaded: " + metadata.getFilename());
        return resource;
    }

    private FileMetadata findMetadataById(String id) {
        return fileMetadataRepository.findById(id).orElseThrow(() -> new StoredFileNotFoundException(id));
    }

    private void rollbackPhysicalFiles(List<FileMetadata> storedFiles, RuntimeException originalException) {
        for (FileMetadata metadata : storedFiles) {
            deletePhysicalFile(metadata.getId(), originalException);
        }
    }

    private void deletePhysicalFile(String id, Exception originalException
    ) {
        try {
            Files.deleteIfExists(
                    uploadDirectory.resolve(id)
            );
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("EMPTY_FILE", "File must not be empty.", Map.of()
            );
        }

        if (file.getSize() > maxSizeBytes) {
            throw new FileValidationException("MAX_SIZE_EXCEEDED", "File too large. Max " + maxSizeBytes + " bytes.", Map.of("limitBytes", maxSizeBytes, "actualBytes", file.getSize()));
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

            throw new FileValidationException("INVALID_FILENAME", "File name contains an invalid path.", Map.of("filename", filename));
        }

        return filename;
    }

    private record ValidatedFile(MultipartFile file, String filename) {
    }
}