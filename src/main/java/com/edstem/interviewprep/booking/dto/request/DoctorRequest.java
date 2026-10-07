package com.edstem.interviewprep.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DoctorRequest(@NotBlank String name) {}
