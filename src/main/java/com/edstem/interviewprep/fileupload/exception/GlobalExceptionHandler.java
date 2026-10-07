package com.edstem.interviewprep.fileupload.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(FileUploadException.class)
  public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException exception) {
    if (exception.getStatus().is5xxServerError()) {
      log.error("File operation failed", exception);
    }
    return ResponseEntity.status(exception.getStatus())
        .body(
            new ErrorResponse(
                exception.getStatus().value(), exception.getCode(), exception.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    log.error("Unexpected error", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred"));
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "FILE_TOO_LARGE",
            "File exceeds the maximum upload size");
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).headers(headers).body(body);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    String message = body instanceof ProblemDetail problem ? problem.getDetail() : null;
    ErrorResponse error =
        new ErrorResponse(
            status.value(), status.name(), message != null ? message : status.getReasonPhrase());
    return ResponseEntity.status(status).headers(headers).body(error);
  }
}
