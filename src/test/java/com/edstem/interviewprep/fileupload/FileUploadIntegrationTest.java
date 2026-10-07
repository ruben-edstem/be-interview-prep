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
import java.awt.image.BufferedImage;
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
        .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FILE_TYPE"));

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
        .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));

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

  @Test
  void pngUploadGetsAThumbnailNoLargerThanTheLimit() throws Exception {
    byte[] png = TestFiles.image("png", 800, 400);

    int id = upload("wide.png", "image/png", png, true);

    byte[] thumbnail =
        mockMvc
            .perform(get("/files/" + id + "/thumbnail"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    BufferedImage image = TestFiles.decode(thumbnail);
    assertEquals(200, image.getWidth());
    assertEquals(100, image.getHeight());
    assertEquals(2, storedFileCount());
  }

  @Test
  void jpegUploadGetsAThumbnailNoLargerThanTheLimit() throws Exception {
    byte[] jpeg = TestFiles.image("jpg", 300, 600);

    int id = upload("tall.jpg", "image/jpeg", jpeg, true);

    byte[] thumbnail =
        mockMvc
            .perform(get("/files/" + id + "/thumbnail"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    BufferedImage image = TestFiles.decode(thumbnail);
    assertEquals(100, image.getWidth());
    assertEquals(200, image.getHeight());
  }

  @Test
  void pdfGetsNoThumbnailAndAskingForOneIsAClearError() throws Exception {
    int id = upload("doc.pdf", "application/pdf", TestFiles.PDF, false);

    mockMvc
        .perform(get("/files/" + id + "/thumbnail"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("THUMBNAIL_NOT_AVAILABLE"));

    assertEquals(1, storedFileCount());
  }

  @Test
  void imageThatCannotBeDecodedIsStoredWithoutAThumbnailAndLeavesNothingHalfStored()
      throws Exception {
    int id = upload("broken.png", "image/png", TestFiles.PNG, false);

    mockMvc
        .perform(get("/files/" + id + "/thumbnail"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("THUMBNAIL_NOT_AVAILABLE"));

    assertEquals(1, storedFileCount());
    assertEquals(1, repository.count());
  }

  @Test
  void deletingAnImageRemovesItsThumbnailToo() throws Exception {
    int id = upload("photo.png", "image/png", TestFiles.image("png", 500, 500), true);
    assertEquals(2, storedFileCount());

    mockMvc.perform(delete("/files/" + id)).andExpect(status().isNoContent());

    assertEquals(0, storedFileCount());
    assertEquals(0, repository.count());
    mockMvc.perform(get("/files/" + id + "/thumbnail")).andExpect(status().isNotFound());
  }

  @Test
  void thumbnailOfATraversalNamedImageStaysInsideTheStorageDirectory() throws Exception {
    upload("../../evil.png", "image/png", TestFiles.image("png", 500, 500), true);

    assertEquals(2, storedFileCount());
    try (Stream<Path> entries = Files.list(base)) {
      assertEquals(1, entries.count());
    }
  }

  private int upload(String name, String contentType, byte[] content, boolean thumbnailExpected)
      throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", name, contentType, content);
    String body =
        mockMvc
            .perform(multipart("/files").file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.thumbnailAvailable").value(thumbnailExpected))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private long storedFileCount() throws IOException {
    try (Stream<Path> files = Files.list(base.resolve("storage"))) {
      return files.count();
    }
  }
}
