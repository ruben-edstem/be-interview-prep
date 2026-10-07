package com.edstem.interviewprep.library.dto.response;

import java.time.Instant;

public record LoanResponse(
    Long id, Long bookId, String memberId, Instant borrowedAt, Instant returnedAt) {}
