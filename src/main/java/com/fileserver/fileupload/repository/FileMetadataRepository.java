package com.fileserver.fileupload.repository;

import com.fileserver.fileupload.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

}
