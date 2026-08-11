package com.fileserver.fileupload.exception;

import java.util.HashMap;
import java.util.Map;

public class FileValidationException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    public FileValidationException(String errorCode,String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }
    public Map<String, Object> getDetails() {
        return details;
    }
}
