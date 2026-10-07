package com.edstem.interviewprep.expenses.dto.response;

import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
    Long id, BigDecimal amount, ExpenseCategory category, LocalDate date, String note) {}
