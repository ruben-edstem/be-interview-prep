package com.edstem.interviewprep.expenses.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.expenses.dto.response.MonthlySummaryResponse;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonthlySummaryCsvMapperTest {

  private final MonthlySummaryCsvMapper mapper = new MonthlySummaryCsvMapper();

  @Test
  void writesHeaderOneRowPerCategoryAndOverallTotal() {
    Map<ExpenseCategory, BigDecimal> totals = zeroTotals();
    totals.put(ExpenseCategory.FOOD, new BigDecimal("0.10"));
    totals.put(ExpenseCategory.TRAVEL, new BigDecimal("0.20"));
    MonthlySummaryResponse summary =
        new MonthlySummaryResponse(YearMonth.of(2026, 3), totals, new BigDecimal("0.30"));

    String csv = mapper.toCsv(summary);

    assertThat(csv)
        .isEqualTo(
            "category,total\r\n"
                + "FOOD,0.10\r\n"
                + "TRAVEL,0.20\r\n"
                + "BILLS,0.00\r\n"
                + "OTHER,0.00\r\n"
                + "TOTAL,0.30\r\n");
  }

  @Test
  void emptyMonthIsAValidFileOfZeros() {
    MonthlySummaryResponse summary =
        new MonthlySummaryResponse(YearMonth.of(2026, 3), zeroTotals(), new BigDecimal("0.00"));

    String csv = mapper.toCsv(summary);

    assertThat(csv)
        .isEqualTo(
            "category,total\r\n"
                + "FOOD,0.00\r\n"
                + "TRAVEL,0.00\r\n"
                + "BILLS,0.00\r\n"
                + "OTHER,0.00\r\n"
                + "TOTAL,0.00\r\n");
  }

  @Test
  void keepsTwoDecimalPlacesForWholeAndLargeAmounts() {
    Map<ExpenseCategory, BigDecimal> totals = zeroTotals();
    totals.put(ExpenseCategory.BILLS, new BigDecimal("1200.00"));
    MonthlySummaryResponse summary =
        new MonthlySummaryResponse(YearMonth.of(2026, 3), totals, new BigDecimal("1200.00"));

    String csv = mapper.toCsv(summary);

    assertThat(csv).contains("BILLS,1200.00\r\n").endsWith("TOTAL,1200.00\r\n");
  }

  private static Map<ExpenseCategory, BigDecimal> zeroTotals() {
    Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
    for (ExpenseCategory category : ExpenseCategory.values()) {
      totals.put(category, new BigDecimal("0.00"));
    }
    return totals;
  }
}
