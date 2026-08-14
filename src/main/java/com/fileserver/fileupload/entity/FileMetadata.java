package com.fileserver.fileupload.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FileMetadata() {
    }

    public FileMetadata(
            String id,
            String filename,
            long size,
            String contentType,
            Instant createdAt
    ) {
        this.id = id;
        this.filename = filename;
        this.size = size;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }



    public String id() {
        return id;
    }

    public String filename() {
        return filename;
    }

    public long size() {
        return size;
    }

    public String contentType() {
        return contentType;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
