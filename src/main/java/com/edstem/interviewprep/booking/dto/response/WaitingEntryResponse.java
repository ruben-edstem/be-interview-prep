package com.edstem.interviewprep.booking.dto.response;

import com.edstem.interviewprep.booking.entity.WaitingStatus;

public record WaitingEntryResponse(
    Long id, Long slotId, String patientName, WaitingStatus status) {}
