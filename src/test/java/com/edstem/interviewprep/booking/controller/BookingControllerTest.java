package com.edstem.interviewprep.booking.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.entity.BookingStatus;
import com.edstem.interviewprep.booking.exception.BookingNotFoundException;
import com.edstem.interviewprep.booking.exception.HoldExpiredException;
import com.edstem.interviewprep.booking.exception.SlotUnavailableException;
import com.edstem.interviewprep.booking.service.BookingService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-10-07T09:05:00Z");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private BookingService bookingService;

  @Test
  void holdReturnsTheCreatedHold() throws Exception {
    when(bookingService.hold(10L, new HoldRequest("Asha")))
        .thenReturn(new BookingResponse(99L, 10L, "Asha", BookingStatus.HELD, EXPIRES_AT));

    mockMvc
        .perform(
            post("/slots/10/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientName\":\"Asha\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.slotId").value(10))
        .andExpect(jsonPath("$.status").value("HELD"))
        .andExpect(jsonPath("$.expiresAt").value("2026-10-07T09:05:00Z"));
  }

  @Test
  void holdRejectsABlankPatientName() throws Exception {
    mockMvc
        .perform(
            post("/slots/10/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientName\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.patientName").value("must not be blank"));

    verifyNoInteractions(bookingService);
  }

  @Test
  void holdReturnsConflictWhenTheSlotIsTaken() throws Exception {
    when(bookingService.hold(10L, new HoldRequest("Asha")))
        .thenThrow(new SlotUnavailableException(10L));

    mockMvc
        .perform(
            post("/slots/10/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientName\":\"Asha\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("SLOT_UNAVAILABLE"));
  }

  @Test
  void confirmReturnsTheConfirmedBooking() throws Exception {
    when(bookingService.confirm(99L))
        .thenReturn(new BookingResponse(99L, 10L, "Asha", BookingStatus.CONFIRMED, EXPIRES_AT));

    mockMvc
        .perform(post("/bookings/99/confirmation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void confirmReturnsConflictWhenTheHoldExpired() throws Exception {
    when(bookingService.confirm(99L)).thenThrow(new HoldExpiredException(99L));

    mockMvc
        .perform(post("/bookings/99/confirmation"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("HOLD_EXPIRED"));
  }

  @Test
  void confirmReturnsNotFoundForAnUnknownBooking() throws Exception {
    when(bookingService.confirm(99L)).thenThrow(new BookingNotFoundException(99L));

    mockMvc
        .perform(post("/bookings/99/confirmation"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"));
  }

  @Test
  void cancelReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/bookings/99")).andExpect(status().isNoContent());

    verify(bookingService).cancel(99L);
  }

  @Test
  void cancelReturnsNotFoundForAnUnknownBooking() throws Exception {
    doThrow(new BookingNotFoundException(99L)).when(bookingService).cancel(99L);

    mockMvc
        .perform(delete("/bookings/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"));
  }
}
