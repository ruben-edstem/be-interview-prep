package com.edstem.interviewprep.ratelimit.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MissingApiKeyException extends ApiException {

  public MissingApiKeyException(String headerName) {
    super(HttpStatus.BAD_REQUEST, "MISSING_API_KEY", "The " + headerName + " header is required.");
  }
}
