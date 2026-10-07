package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends FileUploadException {

  public UnsupportedFileTypeException(String message) {
    super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE", message);
  }
}
