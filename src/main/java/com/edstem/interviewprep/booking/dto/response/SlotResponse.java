package com.edstem.interviewprep.booking.dto.response;

import java.time.LocalDateTime;

public record SlotResponse(
    Long id, Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {}
