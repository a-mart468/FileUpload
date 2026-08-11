package com.fileserver.fileupload.controller;

import com.fileserver.fileupload.model.FileMetadata;
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

import java.nio.charset.StandardCharsets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/files")
public class FileController {

    private  final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<FileMetadata> uploadFile(@RequestParam("file") MultipartFile file) {
        FileMetadata metadata = fileStorageService.store(file);
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
