package com.fileserver.fileupload.exception;

public class StoredFileNotFoundException extends RuntimeException{

    private final String id;

    public StoredFileNotFoundException(String id) {
        super("Could not find file with id " + id);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
