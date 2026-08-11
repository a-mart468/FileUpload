package com.fileserver.fileupload.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFileValidationException(
            FileValidationException exception) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", exception.getErrorCode());
        response.put("message", exception.getMessage());
        response.put("details", exception.getDetails());

        HttpStatus status = switch (exception.getErrorCode()) {
            case "MAX_SIZE_EXCEEDED" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "UNSUPPORTED_MEDIA_TYPE" -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFound(
            StoredFileNotFoundException exception) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "FILE_NOT_FOUND");
        response.put("message", exception.getMessage());
        response.put(
                "details",
                Map.of("id", exception.getId())
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
}