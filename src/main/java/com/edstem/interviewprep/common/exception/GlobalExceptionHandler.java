package com.edstem.interviewprep.common.exception;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    return build(exception.getStatus(), exception.getErrorCode(), exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or invalid");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return build(
        HttpStatus.BAD_REQUEST,
        "INVALID_PARAMETER",
        "Parameter '" + exception.getName() + "' has an invalid value");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception) {
    return build(
        HttpStatus.BAD_REQUEST,
        "MISSING_PARAMETER",
        "Parameter '" + exception.getParameterName() + "' is required");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    log.error("Unexpected error", exception);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorCode, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(status.value(), errorCode, message));
  }
}
