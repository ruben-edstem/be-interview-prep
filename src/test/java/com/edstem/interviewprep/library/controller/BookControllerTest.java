package com.edstem.interviewprep.library.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.exception.GlobalExceptionHandler;
import com.edstem.interviewprep.library.dto.response.BookResponse;
import com.edstem.interviewprep.library.exception.BookBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.exception.DuplicateIsbnException;
import com.edstem.interviewprep.library.service.BookService;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
class BookControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BookService bookService;

  @Test
  void createReturnsCreatedWithTheBook() throws Exception {
    when(bookService.create(any()))
        .thenReturn(new BookResponse(1L, "Clean Code", "Robert Martin", "111", 2008, true));

    mockMvc
        .perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(body("111", 2008)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.available").value(true));
  }

  @Test
  void createRejectsMissingFieldsWithFieldErrors() throws Exception {
    mockMvc
        .perform(post("/books").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.title").exists())
        .andExpect(jsonPath("$.fieldErrors.author").exists())
        .andExpect(jsonPath("$.fieldErrors.isbn").exists())
        .andExpect(jsonPath("$.fieldErrors.publishedYear").exists());

    verifyNoInteractions(bookService);
  }

  @Test
  void createRejectsAPublishedYearInTheFuture() throws Exception {
    int nextYear = Year.now().getValue() + 1;

    mockMvc
        .perform(
            post("/books").contentType(MediaType.APPLICATION_JSON).content(body("111", nextYear)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.publishedYear").exists());

    verifyNoInteractions(bookService);
  }

  @Test
  void createReturnsConflictForADuplicateIsbn() throws Exception {
    when(bookService.create(any())).thenThrow(new DuplicateIsbnException("111"));

    mockMvc
        .perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(body("111", 2008)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ISBN"));
  }

  @Test
  void listPassesTheSearchTerm() throws Exception {
    when(bookService.list("code"))
        .thenReturn(
            List.of(new BookResponse(1L, "Clean Code", "Robert Martin", "111", 2008, true)));

    mockMvc
        .perform(get("/books").param("search", "code"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Clean Code"));
  }

  @Test
  void getReturnsNotFoundInTheErrorFormat() throws Exception {
    when(bookService.get(9L)).thenThrow(new BookNotFoundException(9L));

    mockMvc
        .perform(get("/books/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.errorCode").value("BOOK_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Book 9 was not found"));
  }

  @Test
  void getRejectsANonNumericId() throws Exception {
    mockMvc
        .perform(get("/books/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void updateReturnsTheUpdatedBook() throws Exception {
    when(bookService.update(eq(1L), any()))
        .thenReturn(new BookResponse(1L, "Clean Code", "Robert Martin", "111", 2008, true));

    mockMvc
        .perform(put("/books/1").contentType(MediaType.APPLICATION_JSON).content(body("111", 2008)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isbn").value("111"));
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/books/1")).andExpect(status().isNoContent());

    verify(bookService).delete(1L);
  }

  @Test
  void deleteOfABorrowedBookReturnsConflict() throws Exception {
    doThrow(new BookBorrowedException(1L)).when(bookService).delete(1L);

    mockMvc
        .perform(delete("/books/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("BOOK_CURRENTLY_BORROWED"));
  }

  private String body(String isbn, int year) {
    return """
        {"title":"Clean Code","author":"Robert Martin","isbn":"%s","publishedYear":%d}
        """
        .formatted(isbn, year);
  }
}
