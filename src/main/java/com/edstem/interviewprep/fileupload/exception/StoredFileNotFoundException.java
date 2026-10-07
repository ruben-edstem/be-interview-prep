package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class StoredFileNotFoundException extends FileUploadException {

  public StoredFileNotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", message);
  }
}
