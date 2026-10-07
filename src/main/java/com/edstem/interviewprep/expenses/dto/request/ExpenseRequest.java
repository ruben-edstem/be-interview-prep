package com.edstem.interviewprep.expenses.dto.request;

import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotNull ExpenseCategory category,
    @NotNull LocalDate date,
    @Size(max = 255) String note) {}
