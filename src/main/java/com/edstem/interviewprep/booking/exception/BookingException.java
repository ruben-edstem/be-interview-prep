package com.edstem.interviewprep.booking.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public abstract class BookingException extends ApiException {

  protected BookingException(HttpStatus status, String errorCode, String message) {
    super(status, errorCode, message);
  }
}
