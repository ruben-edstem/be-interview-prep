package com.edstem.interviewprep.expenses.service;

import com.edstem.interviewprep.expenses.dto.request.ExpenseRequest;
import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.entity.Expense;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.exception.ExpenseNotFoundException;
import com.edstem.interviewprep.expenses.exception.InvalidDateRangeException;
import com.edstem.interviewprep.expenses.mapper.ExpenseMapper;
import com.edstem.interviewprep.expenses.repository.ExpenseRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

  private final ExpenseRepository repository;
  private final ExpenseMapper mapper;

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
