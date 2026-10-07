package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends FileUploadException {

  public InvalidFileException(String message) {
    super(HttpStatus.BAD_REQUEST, "INVALID_FILE", message);
  }
}
