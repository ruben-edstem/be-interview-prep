package com.edstem.interviewprep.expenses.repository;

import com.edstem.interviewprep.expenses.entity.Expense;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

  @Query(
      """
      select e from Expense e
      where (cast(:from as date) is null or e.date >= :from)
        and (cast(:to as date) is null or e.date <= :to)
        and (:category is null or e.category = :category)
      order by e.date desc, e.id desc
      """)
  List<Expense> search(
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("category") ExpenseCategory category);

  @Query(
      """
      select e.category as category, sum(e.amount) as total
      from Expense e
      where e.date between :start and :end
      group by e.category
      """)
  List<CategoryTotal> sumByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
