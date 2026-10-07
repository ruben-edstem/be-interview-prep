package com.edstem.interviewprep.library.dto.response;

import java.time.Instant;

public record OverdueLoanResponse(
    Long loanId,
    Long bookId,
    String bookTitle,
    String memberId,
    Instant borrowedAt,
    long daysOverdue) {}
