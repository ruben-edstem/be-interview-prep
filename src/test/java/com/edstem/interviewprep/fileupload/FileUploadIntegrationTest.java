package com.edstem.interviewprep.fileupload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.fileupload.repository.StoredFileRepository;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadIntegrationTest {

  @TempDir static Path base;

  @DynamicPropertySource
  static void storageDirectory(DynamicPropertyRegistry registry) {
    registry.add("app.file-upload.directory", () -> base.resolve("storage").toString());
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private StoredFileRepository repository;

  @BeforeEach
  void clean() throws IOException {
    repository.deleteAll();
    Path storage = base.resolve("storage");
    if (Files.exists(storage)) {
      try (Stream<Path> files = Files.list(storage)) {
        for (Path file : files.toList()) {
          Files.delete(file);
        }
      }
    }
  }

  @Test
  void uploadListDownloadAndDeleteRoundTrip() throws Exception {
    MockMultipartFile png =
        new MockMultipartFile("file", "holiday photo.png", "image/png", TestFiles.PNG);

    String body =
        mockMvc
            .perform(multipart("/files").file(png))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = JsonPath.read(body, "$.id");

    mockMvc
        .perform(get("/files"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].originalName").value("holiday photo.png"))
        .andExpect(jsonPath("$[0].contentType").value("image/png"))
        .andExpect(jsonPath("$[0].size").value(TestFiles.PNG.length))
        .andExpect(jsonPath("$[0].uploadedAt").isNotEmpty());
    mockMvc
        .perform(get("/files/" + id))
        .andExpect(status().isOk())
        .andExpect(content().bytes(TestFiles.PNG))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString("holiday%20photo.png")));
    assertEquals(1, storedFileCount());

    mockMvc.perform(delete("/files/" + id)).andExpect(status().isNoContent());

    assertEquals(0, storedFileCount());
    assertEquals(0, repository.count());
    mockMvc.perform(get("/files/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void executableRenamedToPngIsRejected() throws Exception {
    MockMultipartFile fake =
        new MockMultipartFile("file", "photo.png", "image/png", TestFiles.EXECUTABLE);

    mockMvc
        .perform(multipart("/files").file(fake))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.error").value("UNSUPPORTED_FILE_TYPE"));

    assertEquals(0, storedFileCount());
    assertEquals(0, repository.count());
  }

  @Test
  void fileOverFiveMegabytesIsRejected() throws Exception {
    byte[] content = TestFiles.sized(TestFiles.PNG, 5 * 1024 * 1024 + 1);
    MockMultipartFile big = new MockMultipartFile("file", "big.png", "image/png", content);

    mockMvc
        .perform(multipart("/files").file(big))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"));

    assertEquals(0, repository.count());
  }

  @Test
  void traversalFileNameNeverEscapesTheStorageDirectory() throws Exception {
    MockMultipartFile sneaky =
        new MockMultipartFile("file", "../../evil.png", "image/png", TestFiles.PNG);

    mockMvc
        .perform(multipart("/files").file(sneaky))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.originalName").value("evil.png"));

    assertEquals(1, storedFileCount());
    assertTrue(Files.notExists(base.resolve("evil.png")));
    assertTrue(Files.notExists(base.getParent().resolve("evil.png")));
  }

  private long storedFileCount() throws IOException {
    try (Stream<Path> files = Files.list(base.resolve("storage"))) {
      return files.count();
    }
  }
}
