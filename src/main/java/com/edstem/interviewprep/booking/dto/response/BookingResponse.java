package com.edstem.interviewprep.booking.dto.response;

import com.edstem.interviewprep.booking.entity.BookingStatus;
import java.time.Instant;

public record BookingResponse(
    Long id, Long slotId, String patientName, BookingStatus status, Instant expiresAt) {}
