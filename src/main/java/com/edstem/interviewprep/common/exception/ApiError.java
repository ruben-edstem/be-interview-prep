package com.edstem.interviewprep.common.exception;

import java.util.Map;

public record ApiError(
    int status, String errorCode, String message, Map<String, String> fieldErrors) {

  public static ApiError of(int status, String errorCode, String message) {
    return new ApiError(status, errorCode, message, Map.of());
  }
}
