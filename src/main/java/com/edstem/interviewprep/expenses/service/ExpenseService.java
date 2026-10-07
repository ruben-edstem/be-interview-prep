package com.edstem.interviewprep.expenses.service;

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
import com.edstem.interviewprep.expenses.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

  private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

  private final ExpenseRepository repository;
  private final ExpenseMapper mapper;
  private final MonthlySummaryCsvMapper csvMapper;

  @Transactional
  public ExpenseResponse create(ExpenseRequest request) {
    Expense saved = repository.save(mapper.toEntity(request));
    log.info("Created expense {}", saved.getId());
    return mapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> list(LocalDate from, LocalDate to, ExpenseCategory category) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new InvalidDateRangeException();
    }
    return repository.search(from, to, category).stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public MonthlySummaryResponse summarize(YearMonth month) {
    Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
    for (ExpenseCategory category : ExpenseCategory.values()) {
      totals.put(category, ZERO_AMOUNT);
    }
    repository
        .sumByCategory(month.atDay(1), month.atEndOfMonth())
        .forEach(
            categoryTotal -> totals.put(categoryTotal.getCategory(), categoryTotal.getTotal()));
    BigDecimal overall = totals.values().stream().reduce(ZERO_AMOUNT, BigDecimal::add);
    return new MonthlySummaryResponse(month, totals, overall);
  }

  @Transactional(readOnly = true)
  public CsvFile exportSummary(YearMonth month) {
    String content = csvMapper.toCsv(summarize(month));
    return new CsvFile("expense-summary-" + month + ".csv", content);
  }

  @Transactional
  public ExpenseResponse update(Long id, ExpenseRequest request) {
    Expense expense = repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    mapper.apply(request, expense);
    log.info("Updated expense {}", id);
    return mapper.toResponse(expense);
  }

  @Transactional
  public void delete(Long id) {
    Expense expense = repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    repository.delete(expense);
    log.info("Deleted expense {}", id);
  }
}
