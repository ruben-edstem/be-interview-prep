package com.edstem.interviewprep.expenses.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException {

  public InvalidDateRangeException() {
    super(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "'from' must not be after 'to'");
  }
}
