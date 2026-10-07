package com.edstem.interviewprep.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new StubController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void apiExceptionUsesItsStatusAndErrorCode() throws Exception {
    mockMvc
        .perform(get("/stub/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("STUB_CONFLICT"))
        .andExpect(jsonPath("$.message").value("stub conflict"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  @Test
  void invalidBodyReturnsFieldErrors() throws Exception {
    mockMvc
        .perform(post("/stub/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));
  }

  @Test
  void malformedBodyReturnsBadRequestInTheSameFormat() throws Exception {
    mockMvc
        .perform(post("/stub/validate").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  @Test
  void unsupportedMethodReturnsMethodNotAllowedInTheSameFormat() throws Exception {
    mockMvc
        .perform(post("/stub/conflict"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void unexpectedExceptionHidesTheCause() throws Exception {
    mockMvc
        .perform(get("/stub/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("Unexpected error"));
  }

  @RestController
  static class StubController {

    @GetMapping("/stub/conflict")
    void conflict() {
      throw new StubConflictException();
    }

    @GetMapping("/stub/boom")
    void boom() {
      throw new IllegalStateException("secret internals");
    }

    @PostMapping("/stub/validate")
    void validate(@Valid @RequestBody StubRequest request) {}
  }

  record StubRequest(@NotBlank String name) {}

  static class StubConflictException extends ApiException {

    StubConflictException() {
      super(HttpStatus.CONFLICT, "STUB_CONFLICT", "stub conflict");
    }
  }
}
