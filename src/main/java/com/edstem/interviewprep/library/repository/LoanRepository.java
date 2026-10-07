package com.edstem.interviewprep.library.repository;

import com.edstem.interviewprep.library.entity.Loan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  Optional<Loan> findByBookIdAndReturnedAtIsNull(Long bookId);

  @Query(
      """
      select l from Loan l join fetch l.book
      where l.returnedAt is null and l.borrowedAt < :cutoff
      order by l.borrowedAt
      """)
  List<Loan> findOverdue(@Param("cutoff") Instant cutoff);
}
