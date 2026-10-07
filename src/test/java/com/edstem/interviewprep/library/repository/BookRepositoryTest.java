package com.edstem.interviewprep.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.library.entity.Book;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class BookRepositoryTest {

  @Autowired private BookRepository bookRepository;

  @Test
  void searchMatchesTitleOrAuthorIgnoringCase() {
    Book byTitle = bookRepository.save(book("Clean Code", "Robert Martin", "111"));
    Book byAuthor = bookRepository.save(book("Refactoring", "Martin Fowler", "222"));
    bookRepository.save(book("Effective Java", "Joshua Bloch", "333"));

    List<Book> byCode = bookRepository.search("CODE");
    List<Book> byMartin = bookRepository.search("martin");

    assertThat(byCode).containsExactly(byTitle);
    assertThat(byMartin).containsExactly(byTitle, byAuthor);
  }

  @Test
  void searchWithNoMatchReturnsEmptyList() {
    bookRepository.save(book("Clean Code", "Robert Martin", "111"));

    List<Book> result = bookRepository.search("python");

    assertThat(result).isEmpty();
  }

  @Test
  void isbnMustBeUnique() {
    bookRepository.saveAndFlush(book("Clean Code", "Robert Martin", "111"));

    assertThatThrownBy(() -> bookRepository.saveAndFlush(book("Other", "Someone", "111")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deleteIfAvailableRemovesAnAvailableBook() {
    Book saved = bookRepository.save(book("Clean Code", "Robert Martin", "111"));

    int deleted = bookRepository.deleteIfAvailable(saved.getId());

    assertThat(deleted).isEqualTo(1);
    assertThat(bookRepository.existsById(saved.getId())).isFalse();
  }

  @Test
  void deleteIfAvailableKeepsABorrowedBook() {
    Book borrowed = book("Clean Code", "Robert Martin", "111");
    borrowed.setBorrowed(true);
    Book saved = bookRepository.save(borrowed);

    int deleted = bookRepository.deleteIfAvailable(saved.getId());

    assertThat(deleted).isZero();
    assertThat(bookRepository.existsById(saved.getId())).isTrue();
  }

  private Book book(String title, String author, String isbn) {
    return Book.builder().title(title).author(author).isbn(isbn).publishedYear(2008).build();
  }
}
