package com.fileserver.fileupload.exception;



import com.fileserver.fileupload.entity.ActivityAction;
import com.fileserver.fileupload.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ActivityLogService activityLogService;

    public GlobalExceptionHandler(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>>
    handleFileValidationException(FileValidationException exception, HttpServletRequest request) {
        logFileFailure(request, exception.getErrorCode() + ": " + exception.getMessage());

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
    public ResponseEntity<Map<String, Object>> handleFileNotFound(StoredFileNotFoundException exception, HttpServletRequest request) {
        logFileFailure(request, "FILE_NOT_FOUND: " + exception.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "FILE_NOT_FOUND");
        response.put("message", exception.getMessage());
        response.put("details", Map.of("id", exception.getId()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        String details = exception.getClass().getSimpleName();

        if (exception.getMessage() != null) {
            details += ": " + exception.getMessage();
        }

        logFileFailure(request, details);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", "An unexpected error occurred.");
        response.put("details", Map.of());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void logFileFailure(HttpServletRequest request, String details) {
        String httpMethod = request.getMethod();
        String endpoint = getRequestPath(request);

        ActivityAction actionType = resolveActivityAction(httpMethod, endpoint);

        if (actionType == null) {
            return;
        }

        String username = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();

        String fileId = extractFileId(actionType, endpoint);

        activityLogService.logFailure(username, actionType, httpMethod, endpoint, fileId, limitDetails(details));
    }

    private ActivityAction resolveActivityAction(String httpMethod, String endpoint) {
        if ("POST".equalsIgnoreCase(httpMethod)) {
            if ("/api/files/batch".equals(endpoint)) {
                return ActivityAction.BATCH_UPLOAD;
            }

            if ("/api/files".equals(endpoint)) {
                return ActivityAction.FILE_UPLOAD;
            }
        }

        if ("GET".equalsIgnoreCase(httpMethod)) {
            if ("/api/files".equals(endpoint)) {
                return ActivityAction.FILE_LIST;
            }

            if (endpoint.startsWith("/api/files/")) {
                return ActivityAction.FILE_DOWNLOAD;
            }
        }

        return null;
    }

    private String extractFileId(ActivityAction actionType, String endpoint) {
        if (actionType != ActivityAction.FILE_DOWNLOAD) {
            return null;
        }

        String prefix = "/api/files/";
        String fileId = endpoint.substring(prefix.length());

        if (fileId.isBlank() || fileId.contains("/")) {
            return null;
        }

        return fileId;
    }

    private String getRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.isEmpty()) {
            return uri;
        }

        return uri.substring(contextPath.length());
    }

    private String limitDetails(String details) {
        if (details == null || details.isBlank()) {
            return "Operation failed";
        }

        int maximumLength = 1000;

        if (details.length() <= maximumLength) {
            return details;
        }

        return details.substring(0, maximumLength);
    }
}