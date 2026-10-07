package com.edstem.interviewprep.fileupload.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.fileupload.TestFiles;
import com.edstem.interviewprep.fileupload.dto.response.FileDownload;
import com.edstem.interviewprep.fileupload.dto.response.FileResponse;
import com.edstem.interviewprep.fileupload.exception.FileStorageException;
import com.edstem.interviewprep.fileupload.exception.FileTooLargeException;
import com.edstem.interviewprep.fileupload.exception.StoredFileNotFoundException;
import com.edstem.interviewprep.fileupload.exception.UnsupportedFileTypeException;
import com.edstem.interviewprep.fileupload.service.FileService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(FileController.class)
class FileControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FileService fileService;

  private final MockMultipartFile png =
      new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);

  @Test
  void uploadReturnsCreatedRecord() throws Exception {
    when(fileService.upload(any()))
        .thenReturn(
            new FileResponse(
                1L, "photo.png", "image/png", 64, Instant.parse("2026-01-01T10:00:00Z")));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.originalName").value("photo.png"))
        .andExpect(jsonPath("$.contentType").value("image/png"))
        .andExpect(jsonPath("$.size").value(64))
        .andExpect(jsonPath("$.uploadedAt").value("2026-01-01T10:00:00Z"));
  }

  @Test
  void uploadWithoutFilePartIsABadRequest() throws Exception {
    mockMvc
        .perform(multipart("/files"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void uploadOfOversizedFileReturnsPayloadTooLarge() throws Exception {
    when(fileService.upload(any())).thenThrow(new FileTooLargeException("File is too big"));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.status").value(413))
        .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"))
        .andExpect(jsonPath("$.message").value("File is too big"));
  }

  @Test
  void uploadRejectedByTheMultipartLimitReturnsTheSameErrorShape() throws Exception {
    when(fileService.upload(any())).thenThrow(new MaxUploadSizeExceededException(5));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"));
  }

  @Test
  void uploadOfDisallowedFileReturnsUnsupportedMediaType() throws Exception {
    when(fileService.upload(any())).thenThrow(new UnsupportedFileTypeException("Not allowed"));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.error").value("UNSUPPORTED_FILE_TYPE"))
        .andExpect(jsonPath("$.message").value("Not allowed"));
  }

  @Test
  void listReturnsFileRecords() throws Exception {
    when(fileService.list())
        .thenReturn(List.of(new FileResponse(2L, "doc.pdf", "application/pdf", 10, Instant.EPOCH)));

    mockMvc
        .perform(get("/files"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].originalName").value("doc.pdf"))
        .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
        .andExpect(jsonPath("$[0].size").value(10))
        .andExpect(jsonPath("$[0].uploadedAt").exists());
  }

  @Test
  void downloadReturnsContentWithOriginalFileName() throws Exception {
    when(fileService.download(1L))
        .thenReturn(
            new FileDownload(
                "my photo.png",
                "image/png",
                TestFiles.PNG.length,
                new ByteArrayResource(TestFiles.PNG)));

    mockMvc
        .perform(get("/files/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(TestFiles.PNG))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", org.hamcrest.Matchers.containsString("my%20photo.png")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void downloadOfUnknownFileReturnsNotFound() throws Exception {
    when(fileService.download(9L))
        .thenThrow(new StoredFileNotFoundException("File 9 was not found"));

    mockMvc
        .perform(get("/files/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("FILE_NOT_FOUND"));
  }

  @Test
  void storageFailureReturnsInternalServerErrorWithStorageCode() throws Exception {
    when(fileService.upload(any())).thenThrow(new FileStorageException("Could not store the file"));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("STORAGE_ERROR"))
        .andExpect(jsonPath("$.message").value("Could not store the file"));
  }

  @Test
  void unexpectedFailureReturnsGenericErrorWithoutLeakingDetails() throws Exception {
    when(fileService.upload(any())).thenThrow(new IllegalStateException("jdbc:h2:mem secret"));

    mockMvc
        .perform(multipart("/files").file(png))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/files/1")).andExpect(status().isNoContent());

    verify(fileService).delete(1L);
  }

  @Test
  void deleteOfUnknownFileReturnsNotFound() throws Exception {
    doThrow(new StoredFileNotFoundException("File 9 was not found")).when(fileService).delete(9L);

    mockMvc.perform(delete("/files/9")).andExpect(status().isNotFound());
  }

  @Test
  void nonNumericIdIsABadRequest() throws Exception {
    mockMvc
        .perform(get("/files/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  @Test
  void nonPositiveIdIsABadRequest() throws Exception {
    mockMvc
        .perform(get("/files/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }
}
