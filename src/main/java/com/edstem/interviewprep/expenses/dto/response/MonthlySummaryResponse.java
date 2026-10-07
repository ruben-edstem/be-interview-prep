package com.edstem.interviewprep.expenses.dto.response;

import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record MonthlySummaryResponse(
    YearMonth month, Map<ExpenseCategory, BigDecimal> totals, BigDecimal total) {}
