package com.edstem.interviewprep.expenses.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.edstem.interviewprep.expenses.entity.Expense;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ExpenseRepositoryTest {

  @Autowired private ExpenseRepository repository;

  @Test
  void searchWithoutFiltersReturnsEveryExpenseNewestFirst() {
    Expense older = save("5.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 1));
    Expense newer = save("7.00", ExpenseCategory.BILLS, LocalDate.of(2026, 3, 9));

    List<Expense> found = repository.search(null, null, null);

    assertThat(found).containsExactly(newer, older);
  }

  @Test
  void searchByDateRangeIncludesBothBoundaryDays() {
    save("1.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 9));
    Expense first = save("2.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 10));
    Expense last = save("3.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 20));
    save("4.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 21));

    List<Expense> found =
        repository.search(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20), null);

    assertThat(found).containsExactly(last, first);
  }

  @Test
  void searchByCategoryReturnsOnlyThatCategory() {
    Expense travel = save("20.00", ExpenseCategory.TRAVEL, LocalDate.of(2026, 3, 5));
    save("9.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));

    List<Expense> found = repository.search(null, null, ExpenseCategory.TRAVEL);

    assertThat(found).containsExactly(travel);
  }

  @Test
  void searchCombinesDateRangeAndCategory() {
    Expense match = save("20.00", ExpenseCategory.TRAVEL, LocalDate.of(2026, 3, 5));
    save("9.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));
    save("8.00", ExpenseCategory.TRAVEL, LocalDate.of(2026, 4, 5));

    List<Expense> found =
        repository.search(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), ExpenseCategory.TRAVEL);

    assertThat(found).containsExactly(match);
  }

  @Test
  void sumByCategoryIsExactForDecimalAmounts() {
    save("0.10", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));
    save("0.20", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 6));

    List<CategoryTotal> totals =
        repository.sumByCategory(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    assertThat(totals).hasSize(1);
    assertThat(totals.get(0).getCategory()).isEqualTo(ExpenseCategory.FOOD);
    assertThat(totals.get(0).getTotal()).isEqualTo(new BigDecimal("0.30"));
  }

  @Test
  void sumByCategoryCountsFirstAndLastDayOfMonthButNotNeighbouringDays() {
    save("100.00", ExpenseCategory.BILLS, LocalDate.of(2026, 2, 28));
    save("1.00", ExpenseCategory.BILLS, LocalDate.of(2026, 3, 1));
    save("2.00", ExpenseCategory.BILLS, LocalDate.of(2026, 3, 31));
    save("100.00", ExpenseCategory.BILLS, LocalDate.of(2026, 4, 1));

    List<CategoryTotal> totals =
        repository.sumByCategory(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    assertThat(totals).hasSize(1);
    assertThat(totals.get(0).getTotal()).isEqualTo(new BigDecimal("3.00"));
  }

  @Test
  void sumByCategoryCountsLeapDayAsLastDayOfFebruary() {
    save("4.00", ExpenseCategory.OTHER, LocalDate.of(2028, 2, 29));
    save("100.00", ExpenseCategory.OTHER, LocalDate.of(2028, 3, 1));

    List<CategoryTotal> totals =
        repository.sumByCategory(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29));

    assertThat(totals).hasSize(1);
    assertThat(totals.get(0).getTotal()).isEqualTo(new BigDecimal("4.00"));
  }

  @Test
  void sumByCategoryGroupsPerCategory() {
    save("10.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 5));
    save("15.00", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 6));
    save("40.00", ExpenseCategory.TRAVEL, LocalDate.of(2026, 3, 7));

    List<CategoryTotal> totals =
        repository.sumByCategory(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    assertThat(totals)
        .extracting(CategoryTotal::getCategory, CategoryTotal::getTotal)
        .containsExactlyInAnyOrder(
            tuple(ExpenseCategory.FOOD, new BigDecimal("25.00")),
            tuple(ExpenseCategory.TRAVEL, new BigDecimal("40.00")));
  }

  private Expense save(String amount, ExpenseCategory category, LocalDate date) {
    return repository.saveAndFlush(
        Expense.builder().amount(new BigDecimal(amount)).category(category).date(date).build());
  }
}
