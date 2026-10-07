package com.edstem.interviewprep.fileupload.exception;

import org.springframework.http.HttpStatus;

public class FileTooLargeException extends FileUploadException {

  public FileTooLargeException(String message) {
    super(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", message);
  }
}
