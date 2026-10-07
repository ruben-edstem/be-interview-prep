package com.edstem.interviewprep.booking.controller;

import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @PostMapping("/slots/{slotId}/holds")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse hold(@PathVariable Long slotId, @Valid @RequestBody HoldRequest request) {
    return bookingService.hold(slotId, request);
  }

  @PostMapping("/bookings/{bookingId}/confirmation")
  public BookingResponse confirm(@PathVariable Long bookingId) {
    return bookingService.confirm(bookingId);
  }

  @DeleteMapping("/bookings/{bookingId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long bookingId) {
    bookingService.cancel(bookingId);
  }
}
