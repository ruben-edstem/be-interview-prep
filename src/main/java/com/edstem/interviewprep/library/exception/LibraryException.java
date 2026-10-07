package com.edstem.interviewprep.library.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public abstract class LibraryException extends ApiException {

  protected LibraryException(HttpStatus status, String errorCode, String message) {
    super(status, errorCode, message);
  }
}
