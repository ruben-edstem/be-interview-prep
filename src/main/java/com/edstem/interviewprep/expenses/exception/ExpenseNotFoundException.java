package com.edstem.interviewprep.expenses.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ExpenseNotFoundException extends ApiException {

  public ExpenseNotFoundException(Long id) {
    super(HttpStatus.NOT_FOUND, "EXPENSE_NOT_FOUND", "Expense " + id + " not found");
  }
}
