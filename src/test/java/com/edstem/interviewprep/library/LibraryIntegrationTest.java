package com.edstem.interviewprep.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.library.dto.request.BookRequest;
import com.edstem.interviewprep.library.dto.request.BorrowRequest;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.exception.BookAlreadyBorrowedException;
import com.edstem.interviewprep.library.exception.BookBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.repository.BookRepository;
import com.edstem.interviewprep.library.repository.LoanRepository;
import com.edstem.interviewprep.library.service.BookService;
import com.edstem.interviewprep.library.service.LoanService;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class LibraryIntegrationTest {

  private static final int CONCURRENT_BORROWERS = 8;

  @Autowired private BookService bookService;
  @Autowired private LoanService loanService;
  @Autowired private BookRepository bookRepository;
  @Autowired private LoanRepository loanRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  @AfterEach
  void cleanUp() {
    loanRepository.deleteAll();
    bookRepository.deleteAll();
  }

  @Test
  void onlyOneOfManySimultaneousBorrowersGetsTheBook() throws Exception {
    BookResponse book = createBook("978-0-13-235088-4");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_BORROWERS);
    List<Future<Boolean>> attempts = new ArrayList<>();
    for (int i = 0; i < CONCURRENT_BORROWERS; i++) {
      String member = "member-" + i;
      Callable<Boolean> attempt =
          () -> {
            start.await();
            try {
              loanService.borrow(book.id(), new BorrowRequest(member));
              return true;
            } catch (BookAlreadyBorrowedException ex) {
              return false;
            }
          };
      attempts.add(executor.submit(attempt));
    }

    start.countDown();
    int successes = 0;
    for (Future<Boolean> attempt : attempts) {
      if (attempt.get()) {
        successes++;
      }
    }
    executor.shutdown();

    assertThat(successes).isEqualTo(1);
    assertThat(loanRepository.count()).isEqualTo(1);
    assertThat(bookService.get(book.id()).available()).isFalse();
  }

  @Test
  void aBorrowedBookCanBeBorrowedAgainOnlyAfterItIsReturned() {
    BookResponse book = createBook("978-0-13-235088-4");

    loanService.borrow(book.id(), new BorrowRequest("member-1"));
    assertThatThrownBy(() -> loanService.borrow(book.id(), new BorrowRequest("member-2")))
        .isInstanceOf(BookAlreadyBorrowedException.class);
    loanService.returnBook(book.id());
    loanService.borrow(book.id(), new BorrowRequest("member-2"));

    assertThat(loanRepository.count()).isEqualTo(2);
    assertThat(bookService.get(book.id()).available()).isFalse();
  }

  @Test
  void aBorrowedBookCannotBeDeletedButAReturnedOneCanWithItsHistory() {
    BookResponse book = createBook("978-0-13-235088-4");
    loanService.borrow(book.id(), new BorrowRequest("member-1"));

    assertThatThrownBy(() -> bookService.delete(book.id()))
        .isInstanceOf(BookBorrowedException.class);
    loanService.returnBook(book.id());
    bookService.delete(book.id());

    assertThatThrownBy(() -> bookService.get(book.id())).isInstanceOf(BookNotFoundException.class);
    assertThat(loanRepository.count()).isZero();
  }

  @Test
  void editingABookDoesNotOverwriteABorrowMadeMeanwhile() {
    BookResponse book = createBook("978-0-13-235088-4");
    TransactionTemplate editing = new TransactionTemplate(transactionManager);
    TransactionTemplate separate = new TransactionTemplate(transactionManager);
    separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    editing.executeWithoutResult(
        status -> {
          Book stale = bookRepository.findById(book.id()).orElseThrow();
          separate.executeWithoutResult(
              inner -> loanService.borrow(book.id(), new BorrowRequest("member-1")));
          stale.setTitle("Renamed");
          bookRepository.saveAndFlush(stale);
        });

    BookResponse reloaded = bookService.get(book.id());
    assertThat(reloaded.title()).isEqualTo("Renamed");
    assertThat(reloaded.available()).isFalse();
  }

  private BookResponse createBook(String isbn) {
    return bookService.create(new BookRequest("Clean Code", "Robert Martin", isbn, Year.of(2008)));
  }
}
