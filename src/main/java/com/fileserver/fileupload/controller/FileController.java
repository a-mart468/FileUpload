package com.fileserver.fileupload.controller;

import com.fileserver.fileupload.config.OpenApiConfig;
import com.fileserver.fileupload.entity.FileMetadata;
import com.fileserver.fileupload.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;

import java.util.List;


@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "File upload, batch upload, list and download operations")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class FileController {

    private  final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadata> uploadFile(@RequestPart("file") MultipartFile file) {
        FileMetadata metadata = fileStorageService.store(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }


    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileMetadata>> uploadFiles(
            @RequestPart(value = "files", required = false)
            List<MultipartFile> files) {
        List<FileMetadata> metadata = fileStorageService.storeAll(files);

        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadata>> getAllFiles() {
        return ResponseEntity.ok(fileStorageService.getAllFiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {

        FileMetadata metadata = fileStorageService.getFileById(id);
        Resource resource = fileStorageService.loadFile(id);

        String contentDisposition = ContentDisposition.attachment().filename(metadata.filename(), StandardCharsets.UTF_8).build().toString();

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(metadata.contentType())).contentLength(metadata.size()).header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition).body(resource);
    }
}
