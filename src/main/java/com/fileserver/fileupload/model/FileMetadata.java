package com.fileserver.fileupload.model;

import java.time.Instant;

public record FileMetadata(String id,
                           String filename,
                           long size,
                           String contentType,
                           Instant createdAt) {

}
