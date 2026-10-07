package com.edstem.interviewprep.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record SlotGenerationRequest(
    @NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
