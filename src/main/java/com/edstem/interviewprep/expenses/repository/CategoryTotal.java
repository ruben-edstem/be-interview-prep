package com.edstem.interviewprep.expenses.repository;

import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.math.BigDecimal;

public interface CategoryTotal {

  ExpenseCategory getCategory();

  BigDecimal getTotal();
}
