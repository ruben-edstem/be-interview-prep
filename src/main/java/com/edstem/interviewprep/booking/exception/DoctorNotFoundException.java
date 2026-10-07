package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class DoctorNotFoundException extends BookingException {

  public DoctorNotFoundException(Long id) {
    super(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor %d not found".formatted(id));
  }
}
