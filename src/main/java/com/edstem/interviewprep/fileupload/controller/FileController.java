package com.edstem.interviewprep.fileupload.controller;

import com.edstem.interviewprep.fileupload.dto.response.FileDownload;
import com.edstem.interviewprep.fileupload.dto.response.FileResponse;
import com.edstem.interviewprep.fileupload.service.FileService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public FileResponse upload(@RequestParam("file") @NotNull MultipartFile file) {
    return fileService.upload(file);
  }

  @GetMapping
  public List<FileResponse> list() {
    return fileService.list();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> download(@PathVariable @Positive Long id) {
    FileDownload download = fileService.download(id);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(download.originalName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(download.contentType()))
        .contentLength(download.size())
        .body(download.content());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable @Positive Long id) {
    fileService.delete(id);
  }
}
