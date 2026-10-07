package com.edstem.interviewprep.common.exception;

import com.edstem.interviewprep.common.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    ErrorResponse body =
        new ErrorResponse(
            exception.getStatus().value(), exception.getErrorCode(), exception.getMessage());

    return ResponseEntity.status(exception.getStatus()).headers(exception.headers()).body(body);
  }
}
