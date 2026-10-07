package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends FileUploadException {

  public FileStorageException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message);
  }

  public FileStorageException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message, cause);
  }
}
