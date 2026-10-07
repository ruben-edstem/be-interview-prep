package com.edstem.interviewprep.expenses.mapper;

import com.edstem.interviewprep.expenses.dto.request.ExpenseRequest;
import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.entity.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

  private static final int AMOUNT_SCALE = 2;

  public Expense toEntity(ExpenseRequest request) {
    Expense expense = new Expense();
    apply(request, expense);
    return expense;
  }

  public void apply(ExpenseRequest request, Expense expense) {
    expense.setAmount(request.amount().setScale(AMOUNT_SCALE));
    expense.setCategory(request.category());
    expense.setDate(request.date());
    expense.setNote(request.note());
  }

  public ExpenseResponse toResponse(Expense expense) {
    return new ExpenseResponse(
        expense.getId(),
        expense.getAmount(),
        expense.getCategory(),
        expense.getDate(),
        expense.getNote());
  }
}
