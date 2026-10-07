package com.edstem.interviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.library.dto.request.BookRequest;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.exception.BookBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.exception.DuplicateIsbnException;
import com.edstem.interviewprep.library.mapper.BookMapper;
import com.edstem.interviewprep.library.repository.BookRepository;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock private BookRepository bookRepository;

  private BookService bookService;

  @BeforeEach
  void setUp() {
    bookService = new BookService(bookRepository, new BookMapper());
  }

  @Test
  void createSavesTheBookAndReportsItAvailable() {
    BookRequest request = request("Clean Code", "Robert Martin", "111");
    when(bookRepository.saveAndFlush(any(Book.class)))
        .thenAnswer(
            invocation -> {
              Book book = invocation.getArgument(0);
              book.setId(1L);
              return book;
            });

    BookResponse response = bookService.create(request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.isbn()).isEqualTo("111");
    assertThat(response.publishedYear()).isEqualTo(2008);
    assertThat(response.available()).isTrue();
  }

  @Test
  void createRejectsADuplicateIsbn() {
    BookRequest request = request("Clean Code", "Robert Martin", "111");
    when(bookRepository.saveAndFlush(any(Book.class)))
        .thenThrow(new DataIntegrityViolationException("unique"));

    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(DuplicateIsbnException.class);
  }

  @Test
  void listWithoutSearchReturnsEveryBook() {
    when(bookRepository.findAll(Sort.by("id"))).thenReturn(List.of(book(1L), book(2L)));

    List<BookResponse> responses = bookService.list(null);

    assertThat(responses).extracting(BookResponse::id).containsExactly(1L, 2L);
    verify(bookRepository, never()).search(any());
  }

  @Test
  void listWithBlankSearchReturnsEveryBook() {
    when(bookRepository.findAll(Sort.by("id"))).thenReturn(List.of(book(1L)));

    List<BookResponse> responses = bookService.list("   ");

    assertThat(responses).hasSize(1);
    verify(bookRepository, never()).search(any());
  }

  @Test
  void listWithSearchUsesTheTrimmedTerm() {
    when(bookRepository.search("code")).thenReturn(List.of(book(1L)));

    List<BookResponse> responses = bookService.list(" code ");

    assertThat(responses).extracting(BookResponse::id).containsExactly(1L);
  }

  @Test
  void getThrowsWhenTheBookDoesNotExist() {
    when(bookRepository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookService.get(9L)).isInstanceOf(BookNotFoundException.class);
  }

  @Test
  void updateChangesTheFieldsOfTheExistingBook() {
    Book existing = book(1L);
    when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(bookRepository.saveAndFlush(existing)).thenReturn(existing);

    BookResponse response = bookService.update(1L, request("New Title", "New Author", "999"));

    assertThat(response.title()).isEqualTo("New Title");
    assertThat(response.author()).isEqualTo("New Author");
    assertThat(response.isbn()).isEqualTo("999");
  }

  @Test
  void updateRejectsAnIsbnUsedByAnotherBook() {
    Book existing = book(1L);
    when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(bookRepository.saveAndFlush(existing))
        .thenThrow(new DataIntegrityViolationException("unique"));

    assertThatThrownBy(() -> bookService.update(1L, request("T", "A", "222")))
        .isInstanceOf(DuplicateIsbnException.class);
  }

  @Test
  void updateThrowsWhenTheBookDoesNotExist() {
    when(bookRepository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookService.update(9L, request("T", "A", "222")))
        .isInstanceOf(BookNotFoundException.class);
  }

  @Test
  void deleteSucceedsWhenTheBookIsAvailable() {
    when(bookRepository.deleteIfAvailable(1L)).thenReturn(1);

    bookService.delete(1L);

    verify(bookRepository).deleteIfAvailable(1L);
  }

  @Test
  void deleteRejectsABorrowedBook() {
    when(bookRepository.deleteIfAvailable(1L)).thenReturn(0);
    when(bookRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> bookService.delete(1L)).isInstanceOf(BookBorrowedException.class);
  }

  @Test
  void deleteThrowsWhenTheBookDoesNotExist() {
    when(bookRepository.deleteIfAvailable(9L)).thenReturn(0);
    when(bookRepository.existsById(9L)).thenReturn(false);

    assertThatThrownBy(() -> bookService.delete(9L)).isInstanceOf(BookNotFoundException.class);
  }

  private BookRequest request(String title, String author, String isbn) {
    return new BookRequest(title, author, isbn, Year.of(2008));
  }

  private Book book(Long id) {
    return Book.builder()
        .id(id)
        .title("Clean Code")
        .author("Robert Martin")
        .isbn("111")
        .publishedYear(2008)
        .build();
  }
}
