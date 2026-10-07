package com.edstem.interviewprep.common.exception;

import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException ex) {
    ApiError body = ApiError.of(ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
    return ResponseEntity.status(ex.getStatus()).headers(ex.headers()).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ApiError body = ApiError.of(status.value(), status.name(), "Unexpected error");
    return ResponseEntity.status(status).body(body);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> fieldErrors = new TreeMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    ApiError body =
        new ApiError(status.value(), "VALIDATION_FAILED", "Request validation failed", fieldErrors);
    return ResponseEntity.status(status).headers(headers).body(body);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    String detail = body instanceof ProblemDetail problem ? problem.getDetail() : null;
    String message = detail != null ? detail : status.getReasonPhrase();
    ApiError error = ApiError.of(status.value(), status.name(), message);
    return ResponseEntity.status(statusCode).headers(headers).body(error);
  }
}
