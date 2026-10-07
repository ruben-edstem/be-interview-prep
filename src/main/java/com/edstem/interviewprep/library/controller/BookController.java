package com.edstem.interviewprep.library.controller;

import com.edstem.interviewprep.library.dto.request.BookRequest;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookResponse create(@Valid @RequestBody BookRequest request) {
    return bookService.create(request);
  }

  @GetMapping
  public List<BookResponse> list(@RequestParam(required = false) String search) {
    return bookService.list(search);
  }

  @GetMapping("/{id}")
  public BookResponse get(@PathVariable Long id) {
    return bookService.get(id);
  }

  @PutMapping("/{id}")
  public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
    return bookService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    bookService.delete(id);
  }
}
