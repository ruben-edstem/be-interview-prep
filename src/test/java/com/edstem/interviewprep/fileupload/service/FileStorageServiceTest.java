package com.edstem.interviewprep.fileupload.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.fileupload.TestFiles;
import com.edstem.interviewprep.fileupload.config.FileUploadProperties;
import com.edstem.interviewprep.fileupload.exception.FileStorageException;
import com.edstem.interviewprep.fileupload.exception.StoredFileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceTest {

  @TempDir Path base;

  private Path storageRoot;
  private FileStorageService storage;

  @BeforeEach
  void setUp() {
    storageRoot = base.resolve("storage");
    storage =
        new FileStorageService(new FileUploadProperties(storageRoot, DataSize.ofMegabytes(5)));
  }

  @Test
  void storeWritesContentUnderTheStorageRootWithAGeneratedName() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "../../photo.png", "image/png", TestFiles.PNG);

    String key = storage.store(file);

    Path stored = storageRoot.resolve(key);
    assertTrue(Files.isRegularFile(stored));
    assertArrayEquals(TestFiles.PNG, Files.readAllBytes(stored));
    assertFalse(key.contains("photo"));
  }

  @Test
  void loadReturnsStoredContent() throws IOException {
    String key = storage.store(new MockMultipartFile("file", "a.png", "image/png", TestFiles.PNG));

    Resource resource = storage.load(key);

    assertArrayEquals(TestFiles.PNG, resource.getInputStream().readAllBytes());
  }

  @Test
  void loadOfMissingContentFails() {
    assertThrows(StoredFileNotFoundException.class, () -> storage.load("missing"));
  }

  @Test
  void deleteRemovesTheFile() throws IOException {
    String key = storage.store(new MockMultipartFile("file", "a.png", "image/png", TestFiles.PNG));

    storage.delete(key);

    try (Stream<Path> files = Files.list(storageRoot)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void keysEscapingTheStorageRootAreRejected() throws IOException {
    Path outside = Files.write(base.resolve("secret.txt"), new byte[] {1});

    assertThrows(FileStorageException.class, () -> storage.load("../secret.txt"));
    assertThrows(FileStorageException.class, () -> storage.delete("../secret.txt"));
    assertThrows(FileStorageException.class, () -> storage.load(outside.toString()));
    assertThrows(FileStorageException.class, () -> storage.delete(".."));
    assertTrue(Files.exists(outside));
  }

  @Test
  void creatingTheStorageDirectoryFailsWhenTheRootIsAFile() throws IOException {
    Path notADirectory = Files.writeString(base.resolve("not-a-directory"), "x");
    FileUploadProperties properties =
        new FileUploadProperties(notADirectory, DataSize.ofMegabytes(5));

    assertThrows(FileStorageException.class, () -> new FileStorageService(properties));
  }

  @Test
  void storeFailsAndLeavesNothingBehindWhenTheUploadCannotBeRead() throws IOException {
    MultipartFile unreadable = mock(MultipartFile.class);
    when(unreadable.getInputStream()).thenThrow(new IOException("disk error"));

    assertThrows(FileStorageException.class, () -> storage.store(unreadable));

    try (Stream<Path> files = Files.list(storageRoot)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void deleteFailsWhenTheFileCannotBeRemoved() throws IOException {
    Path directory = Files.createDirectories(storageRoot.resolve("locked"));
    Files.writeString(directory.resolve("inner"), "x");

    assertThrows(FileStorageException.class, () -> storage.delete("locked"));
  }
}
