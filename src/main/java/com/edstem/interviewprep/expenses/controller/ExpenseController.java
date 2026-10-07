package com.edstem.interviewprep.expenses.controller;

import com.edstem.interviewprep.expenses.dto.request.ExpenseRequest;
import com.edstem.interviewprep.expenses.dto.response.CsvFile;
import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.dto.response.MonthlySummaryResponse;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

  private final ExpenseService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
    return service.create(request);
  }

  @GetMapping
  public List<ExpenseResponse> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) ExpenseCategory category) {
    return service.list(from, to, category);
  }

  @GetMapping("/summary")
  public MonthlySummaryResponse summary(
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
    return service.summarize(month);
  }

  @GetMapping("/summary/csv")
  public ResponseEntity<String> summaryCsv(
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
    CsvFile file = service.exportSummary(month);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.fileName()).build().toString())
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(file.content());
  }

  @PutMapping("/{id}")
  public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
