package com.edstem.interviewprep.expenses;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.expenses.entity.Expense;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseSummaryCsvIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseRepository repository;

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
  }

  @Test
  void fileCountsFirstAndLastDayOfMonthAndNothingFromNeighbouringMonths() throws Exception {
    save("100.00", ExpenseCategory.BILLS, LocalDate.of(2026, 2, 28));
    save("1.00", ExpenseCategory.BILLS, LocalDate.of(2026, 3, 1));
    save("2.00", ExpenseCategory.BILLS, LocalDate.of(2026, 3, 31));
    save("100.00", ExpenseCategory.BILLS, LocalDate.of(2026, 4, 1));

    mockMvc
        .perform(get("/expenses/summary/csv").param("month", "2026-03"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    "category,total\r\n"
                        + "FOOD,0.00\r\n"
                        + "TRAVEL,0.00\r\n"
                        + "BILLS,3.00\r\n"
                        + "OTHER,0.00\r\n"
                        + "TOTAL,3.00\r\n"));
  }

  @Test
  void fileTotalsAreExact() throws Exception {
    save("0.10", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));
    save("0.20", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 6));

    mockMvc
        .perform(get("/expenses/summary/csv").param("month", "2026-03"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    "category,total\r\n"
                        + "FOOD,0.30\r\n"
                        + "TRAVEL,0.00\r\n"
                        + "BILLS,0.00\r\n"
                        + "OTHER,0.00\r\n"
                        + "TOTAL,0.30\r\n"));
  }

  @Test
  void monthWithoutExpensesGivesFileOfZeros() throws Exception {
    save("5.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));

    mockMvc
        .perform(get("/expenses/summary/csv").param("month", "2026-07"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    "category,total\r\n"
                        + "FOOD,0.00\r\n"
                        + "TRAVEL,0.00\r\n"
                        + "BILLS,0.00\r\n"
                        + "OTHER,0.00\r\n"
                        + "TOTAL,0.00\r\n"));
  }

  private void save(String amount, ExpenseCategory category, LocalDate date) {
    repository.save(
        Expense.builder().amount(new BigDecimal(amount)).category(category).date(date).build());
  }
}
