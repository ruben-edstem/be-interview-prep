package com.edstem.interviewprep.fileupload.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public abstract class FileUploadException extends ApiException {

  protected FileUploadException(HttpStatus status, String errorCode, String message) {
    super(status, errorCode, message);
  }

  protected FileUploadException(
      HttpStatus status, String errorCode, String message, Throwable cause) {
    super(status, errorCode, message, cause);
  }
}
