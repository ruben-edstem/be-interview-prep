package com.edstem.interviewprep.fileupload.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class FileUploadException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  protected FileUploadException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  protected FileUploadException(HttpStatus status, String code, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
  }
}
