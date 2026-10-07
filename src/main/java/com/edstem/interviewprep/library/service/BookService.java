package com.edstem.interviewprep.library.service;

import com.edstem.interviewprep.library.dto.request.BookRequest;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.exception.BookBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.exception.DuplicateIsbnException;
import com.edstem.interviewprep.library.mapper.BookMapper;
import com.edstem.interviewprep.library.repository.BookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BookService {

  private final BookRepository bookRepository;
  private final BookMapper bookMapper;

  @Transactional
  public BookResponse create(BookRequest request) {
    Book book = bookMapper.toEntity(request);
    return bookMapper.toResponse(saveUniqueIsbn(book));
  }

  @Transactional(readOnly = true)
  public List<BookResponse> list(String search) {
    List<Book> books =
        StringUtils.hasText(search)
            ? bookRepository.search(search.trim())
            : bookRepository.findAll(Sort.by("id"));
    return books.stream().map(bookMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public BookResponse get(Long id) {
    return bookMapper.toResponse(findBook(id));
  }

  @Transactional
  public BookResponse update(Long id, BookRequest request) {
    Book book = findBook(id);
    bookMapper.apply(book, request);
    return bookMapper.toResponse(saveUniqueIsbn(book));
  }

  @Transactional
  public void delete(Long id) {
    if (bookRepository.deleteIfAvailable(id) == 0) {
      throw bookRepository.existsById(id)
          ? new BookBorrowedException(id)
          : new BookNotFoundException(id);
    }
  }

  private Book findBook(Long id) {
    return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
  }

  private Book saveUniqueIsbn(Book book) {
    try {
      return bookRepository.saveAndFlush(book);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateIsbnException(book.getIsbn());
    }
  }
}
