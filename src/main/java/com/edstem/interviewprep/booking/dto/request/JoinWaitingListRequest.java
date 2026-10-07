package com.edstem.interviewprep.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinWaitingListRequest(@NotBlank String patientName) {}
