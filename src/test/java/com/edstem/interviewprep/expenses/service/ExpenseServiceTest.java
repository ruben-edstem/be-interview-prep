package com.edstem.interviewprep.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.expenses.dto.request.ExpenseRequest;
import com.edstem.interviewprep.expenses.dto.response.CsvFile;
import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.dto.response.MonthlySummaryResponse;
import com.edstem.interviewprep.expenses.entity.Expense;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.exception.ExpenseNotFoundException;
import com.edstem.interviewprep.expenses.exception.InvalidDateRangeException;
import com.edstem.interviewprep.expenses.mapper.ExpenseMapper;
import com.edstem.interviewprep.expenses.mapper.MonthlySummaryCsvMapper;
import com.edstem.interviewprep.expenses.repository.CategoryTotal;
import com.edstem.interviewprep.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  @Mock private ExpenseRepository repository;

  private ExpenseService service;

  @BeforeEach
  void setUp() {
    service = new ExpenseService(repository, new ExpenseMapper(), new MonthlySummaryCsvMapper());
  }

  @Test
  void createStoresAmountWithTwoDecimalPlaces() {
    ExpenseRequest request =
        new ExpenseRequest(new BigDecimal("12.5"), ExpenseCategory.FOOD, date(2026, 3, 1), "Lunch");
    when(repository.save(any(Expense.class)))
        .thenAnswer(
            invocation -> {
              Expense expense = invocation.getArgument(0);
              expense.setId(1L);
              return expense;
            });

    ExpenseResponse response = service.create(request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.amount()).isEqualTo(new BigDecimal("12.50"));
    assertThat(response.category()).isEqualTo(ExpenseCategory.FOOD);
    assertThat(response.note()).isEqualTo("Lunch");
  }

  @Test
  void listPassesFiltersToRepository() {
    Expense expense = expense(1L, "9.99", ExpenseCategory.TRAVEL, date(2026, 3, 5));
    when(repository.search(date(2026, 3, 1), date(2026, 3, 31), ExpenseCategory.TRAVEL))
        .thenReturn(List.of(expense));

    List<ExpenseResponse> responses =
        service.list(date(2026, 3, 1), date(2026, 3, 31), ExpenseCategory.TRAVEL);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).amount()).isEqualTo(new BigDecimal("9.99"));
  }

  @Test
  void listAcceptsSingleDayRange() {
    when(repository.search(date(2026, 3, 1), date(2026, 3, 1), null)).thenReturn(List.of());

    List<ExpenseResponse> responses = service.list(date(2026, 3, 1), date(2026, 3, 1), null);

    assertThat(responses).isEmpty();
  }

  @Test
  void listRejectsRangeWhereFromIsAfterTo() {
    assertThrows(
        InvalidDateRangeException.class,
        () -> service.list(date(2026, 3, 2), date(2026, 3, 1), null));

    verify(repository, never()).search(any(), any(), any());
  }

  @Test
  void updateReplacesAllFields() {
    Expense existing = expense(7L, "5.00", ExpenseCategory.FOOD, date(2026, 3, 1));
    when(repository.findById(7L)).thenReturn(Optional.of(existing));
    ExpenseRequest request =
        new ExpenseRequest(new BigDecimal("8.25"), ExpenseCategory.BILLS, date(2026, 3, 2), null);

    ExpenseResponse response = service.update(7L, request);

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.amount()).isEqualTo(new BigDecimal("8.25"));
    assertThat(response.category()).isEqualTo(ExpenseCategory.BILLS);
    assertThat(response.date()).isEqualTo(date(2026, 3, 2));
    assertThat(response.note()).isNull();
  }

  @Test
  void updateOfMissingExpenseThrowsNotFound() {
    when(repository.findById(99L)).thenReturn(Optional.empty());
    ExpenseRequest request =
        new ExpenseRequest(new BigDecimal("1.00"), ExpenseCategory.OTHER, date(2026, 3, 2), null);

    assertThrows(ExpenseNotFoundException.class, () -> service.update(99L, request));
  }

  @Test
  void deleteRemovesExistingExpense() {
    Expense existing = expense(3L, "5.00", ExpenseCategory.FOOD, date(2026, 3, 1));
    when(repository.findById(3L)).thenReturn(Optional.of(existing));

    service.delete(3L);

    verify(repository).delete(existing);
  }

  @Test
  void deleteOfMissingExpenseThrowsNotFound() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ExpenseNotFoundException.class, () -> service.delete(99L));

    verify(repository, never()).delete(any(Expense.class));
  }

  @Test
  void summarizeAddsCategoryTotalsExactly() {
    when(repository.sumByCategory(date(2026, 3, 1), date(2026, 3, 31)))
        .thenReturn(
            List.of(
                new Total(ExpenseCategory.FOOD, new BigDecimal("0.10")),
                new Total(ExpenseCategory.TRAVEL, new BigDecimal("0.20"))));

    MonthlySummaryResponse summary = service.summarize(YearMonth.of(2026, 3));

    assertThat(summary.month()).isEqualTo(YearMonth.of(2026, 3));
    assertThat(summary.totals().get(ExpenseCategory.FOOD)).isEqualTo(new BigDecimal("0.10"));
    assertThat(summary.totals().get(ExpenseCategory.TRAVEL)).isEqualTo(new BigDecimal("0.20"));
    assertThat(summary.total()).isEqualTo(new BigDecimal("0.30"));
  }

  @Test
  void summarizeListsEveryCategoryWithZeroForEmptyMonth() {
    when(repository.sumByCategory(date(2026, 3, 1), date(2026, 3, 31))).thenReturn(List.of());

    MonthlySummaryResponse summary = service.summarize(YearMonth.of(2026, 3));

    assertThat(summary.totals().keySet()).containsExactly(ExpenseCategory.values());
    assertThat(summary.totals().values()).allMatch(amount -> amount.equals(new BigDecimal("0.00")));
    assertThat(summary.total()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void summarizeCoversFirstToLastDayOfLeapFebruary() {
    when(repository.sumByCategory(date(2028, 2, 1), date(2028, 2, 29))).thenReturn(List.of());

    service.summarize(YearMonth.of(2028, 2));

    verify(repository).sumByCategory(date(2028, 2, 1), date(2028, 2, 29));
  }

  @Test
  void summarizeCoversFirstToLastDayOfThirtyDayMonth() {
    when(repository.sumByCategory(date(2026, 4, 1), date(2026, 4, 30))).thenReturn(List.of());

    service.summarize(YearMonth.of(2026, 4));

    verify(repository).sumByCategory(date(2026, 4, 1), date(2026, 4, 30));
  }

  @Test
  void exportSummaryNamesFileAfterMonthAndWritesExactTotals() {
    when(repository.sumByCategory(date(2026, 3, 1), date(2026, 3, 31)))
        .thenReturn(
            List.of(
                new Total(ExpenseCategory.FOOD, new BigDecimal("0.10")),
                new Total(ExpenseCategory.TRAVEL, new BigDecimal("0.20"))));

    CsvFile file = service.exportSummary(YearMonth.of(2026, 3));

    assertThat(file.fileName()).isEqualTo("expense-summary-2026-03.csv");
    assertThat(file.content())
        .isEqualTo(
            "category,total\r\n"
                + "FOOD,0.10\r\n"
                + "TRAVEL,0.20\r\n"
                + "BILLS,0.00\r\n"
                + "OTHER,0.00\r\n"
                + "TOTAL,0.30\r\n");
  }

  @Test
  void exportSummaryOfEmptyMonthIsAllZeros() {
    when(repository.sumByCategory(date(2028, 2, 1), date(2028, 2, 29))).thenReturn(List.of());

    CsvFile file = service.exportSummary(YearMonth.of(2028, 2));

    assertThat(file.fileName()).isEqualTo("expense-summary-2028-02.csv");
    assertThat(file.content()).endsWith("OTHER,0.00\r\nTOTAL,0.00\r\n");
  }

  private record Total(ExpenseCategory category, BigDecimal total) implements CategoryTotal {

    @Override
    public ExpenseCategory getCategory() {
      return category;
    }

    @Override
    public BigDecimal getTotal() {
      return total;
    }
  }

  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  private static Expense expense(Long id, String amount, ExpenseCategory category, LocalDate date) {
    return Expense.builder()
        .id(id)
        .amount(new BigDecimal(amount))
        .category(category)
        .date(date)
        .build();
  }
}
