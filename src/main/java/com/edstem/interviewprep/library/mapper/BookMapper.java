package com.edstem.interviewprep.library.mapper;

import com.edstem.interviewprep.library.dto.request.BookRequest;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

  public Book toEntity(BookRequest request) {
    Book book = new Book();
    apply(book, request);
    return book;
  }

  public void apply(Book book, BookRequest request) {
    book.setTitle(request.title().trim());
    book.setAuthor(request.author().trim());
    book.setIsbn(request.isbn().trim());
    book.setPublishedYear(request.publishedYear().getValue());
  }

  public BookResponse toResponse(Book book) {
    return new BookResponse(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getIsbn(),
        book.getPublishedYear(),
        !book.isBorrowed());
  }
}
