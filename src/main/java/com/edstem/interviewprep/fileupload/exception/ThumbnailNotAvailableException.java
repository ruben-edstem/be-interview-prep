package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class ThumbnailNotAvailableException extends FileUploadException {

  public ThumbnailNotAvailableException(String message) {
    super(HttpStatus.NOT_FOUND, "THUMBNAIL_NOT_AVAILABLE", message);
  }
}
