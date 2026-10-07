package com.edstem.interviewprep.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.entity.Loan;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class LoanRepositoryTest {

  private static final Instant CUTOFF = Instant.parse("2026-10-06T12:00:00Z");

  @Autowired private BookRepository bookRepository;
  @Autowired private LoanRepository loanRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  @Test
  void findsOnlyTheOpenLoanOfABook() {
    Book book = bookRepository.save(book("111"));
    Loan returned = loan(book, "member-1");
    returned.setReturnedAt(Instant.now());
    loanRepository.save(returned);
    Loan open = loanRepository.save(loan(book, "member-2"));

    Optional<Loan> found = loanRepository.findByBookIdAndReturnedAtIsNull(book.getId());

    assertThat(found).contains(open);
  }

  @Test
  void findsNothingWhenEveryLoanIsReturned() {
    Book book = bookRepository.save(book("111"));
    Loan returned = loan(book, "member-1");
    returned.setReturnedAt(Instant.now());
    loanRepository.save(returned);

    Optional<Loan> found = loanRepository.findByBookIdAndReturnedAtIsNull(book.getId());

    assertThat(found).isEmpty();
  }

  @Test
  void setsTheBorrowTimeOnCreation() {
    Book book = bookRepository.save(book("111"));

    Loan saved = loanRepository.saveAndFlush(loan(book, "member-1"));

    assertThat(saved.getBorrowedAt()).isNotNull();
    assertThat(saved.getReturnedAt()).isNull();
  }

  @Test
  void findsAnOpenLoanBorrowedBeforeTheCutoffTogetherWithItsBook() {
    Long loanId = borrowedAt("111", CUTOFF.minusSeconds(1));

    List<Loan> overdue = loanRepository.findOverdue(CUTOFF);

    assertThat(overdue).extracting(Loan::getId).containsExactly(loanId);
    assertThat(Hibernate.isInitialized(overdue.get(0).getBook())).isTrue();
  }

  @Test
  void aLoanBorrowedExactlyAtTheCutoffIsNotOverdue() {
    borrowedAt("111", CUTOFF);

    List<Loan> overdue = loanRepository.findOverdue(CUTOFF);

    assertThat(overdue).isEmpty();
  }

  @Test
  void aLoanBorrowedAfterTheCutoffIsNotOverdue() {
    borrowedAt("111", CUTOFF.plusSeconds(1));

    List<Loan> overdue = loanRepository.findOverdue(CUTOFF);

    assertThat(overdue).isEmpty();
  }

  @Test
  void aReturnedLoanIsNotOverdueEvenWhenItCameBackLate() {
    Long loanId = borrowedAt("111", CUTOFF.minus(Duration.ofDays(30)));
    returnedAt(loanId, CUTOFF.minus(Duration.ofDays(5)));

    List<Loan> overdue = loanRepository.findOverdue(CUTOFF);

    assertThat(overdue).isEmpty();
  }

  @Test
  void listsTheOldestOverdueLoanFirst() {
    Long newer = borrowedAt("111", CUTOFF.minus(Duration.ofDays(1)));
    Long older = borrowedAt("222", CUTOFF.minus(Duration.ofDays(9)));

    List<Loan> overdue = loanRepository.findOverdue(CUTOFF);

    assertThat(overdue).extracting(Loan::getId).containsExactly(older, newer);
  }

  private Long borrowedAt(String isbn, Instant borrowedAt) {
    Book book = bookRepository.save(book(isbn));
    Loan loan = loanRepository.saveAndFlush(loan(book, "member-1"));
    jdbcTemplate.update(
        "update loan set borrowed_at = ? where id = ?", Timestamp.from(borrowedAt), loan.getId());
    entityManager.clear();
    return loan.getId();
  }

  private void returnedAt(Long loanId, Instant returnedAt) {
    jdbcTemplate.update(
        "update loan set returned_at = ? where id = ?", Timestamp.from(returnedAt), loanId);
    entityManager.clear();
  }

  private Book book(String isbn) {
    return Book.builder()
        .title("Clean Code")
        .author("Robert Martin")
        .isbn(isbn)
        .publishedYear(2008)
        .build();
  }

  private Loan loan(Book book, String memberId) {
    return Loan.builder().book(book).memberId(memberId).build();
  }
}
