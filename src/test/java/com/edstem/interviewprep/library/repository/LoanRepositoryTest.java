package com.edstem.interviewprep.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.entity.Loan;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class LoanRepositoryTest {

  @Autowired private BookRepository bookRepository;
  @Autowired private LoanRepository loanRepository;

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
