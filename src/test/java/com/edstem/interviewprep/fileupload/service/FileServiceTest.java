package com.edstem.interviewprep.fileupload.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.fileupload.TestFiles;
import com.edstem.interviewprep.fileupload.config.FileUploadProperties;
import com.edstem.interviewprep.fileupload.dto.response.FileDownload;
import com.edstem.interviewprep.fileupload.dto.response.FileResponse;
import com.edstem.interviewprep.fileupload.entity.FileType;
import com.edstem.interviewprep.fileupload.entity.StoredFile;
import com.edstem.interviewprep.fileupload.exception.FileStorageException;
import com.edstem.interviewprep.fileupload.exception.FileTooLargeException;
import com.edstem.interviewprep.fileupload.exception.InvalidFileException;
import com.edstem.interviewprep.fileupload.exception.StoredFileNotFoundException;
import com.edstem.interviewprep.fileupload.exception.UnsupportedFileTypeException;
import com.edstem.interviewprep.fileupload.repository.StoredFileRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final int LIMIT_BYTES = 5 * 1024 * 1024;

  @Mock private StoredFileRepository repository;
  @Mock private FileStorageService storage;
  @Mock private ThumbnailGenerator generator;

  private FileService service;

  @BeforeEach
  void setUp() {
    FileUploadProperties properties =
        new FileUploadProperties(Path.of("unused"), DataSize.ofMegabytes(5));
    service = new FileService(repository, storage, properties, generator);
  }

  @Test
  void uploadStoresPngAndRecordsMetadata() {
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    ArgumentCaptor<StoredFile> saved = ArgumentCaptor.forClass(StoredFile.class);
    verify(repository).saveAndFlush(saved.capture());
    assertEquals("photo.png", response.originalName());
    assertEquals("image/png", response.contentType());
    assertEquals(TestFiles.PNG.length, response.size());
    assertEquals("key-1", saved.getValue().getStorageKey());
    assertEquals(FileType.PNG, saved.getValue().getFileType());
  }

  @Test
  void uploadAcceptsJpegAndPdf() {
    MockMultipartFile jpeg =
        new MockMultipartFile("file", "Photo.JPG", "image/jpeg", TestFiles.JPEG);
    MockMultipartFile pdf =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", TestFiles.PDF);
    when(storage.store(any())).thenReturn("key");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse jpegResponse = service.upload(jpeg);
    FileResponse pdfResponse = service.upload(pdf);

    assertEquals("image/jpeg", jpegResponse.contentType());
    assertEquals("application/pdf", pdfResponse.contentType());
  }

  @Test
  void uploadAcceptsFileExactlyAtTheLimit() {
    byte[] content = TestFiles.sized(TestFiles.PNG, LIMIT_BYTES);
    MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", content);
    when(storage.store(file)).thenReturn("key");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    assertEquals(LIMIT_BYTES, response.size());
  }

  @Test
  void uploadRejectsFileOverTheLimit() {
    byte[] content = TestFiles.sized(TestFiles.PNG, LIMIT_BYTES + 1);
    MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", content);

    assertThrows(FileTooLargeException.class, () -> service.upload(file));

    verifyNoInteractions(storage, repository);
  }

  @Test
  void uploadRejectsExecutableRenamedToPng() {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.png", "image/png", TestFiles.EXECUTABLE);

    assertThrows(UnsupportedFileTypeException.class, () -> service.upload(file));

    verifyNoInteractions(storage, repository);
  }

  @Test
  void uploadRejectsDisallowedExtensionEvenWithImageContent() {
    MockMultipartFile file = new MockMultipartFile("file", "photo.exe", "image/png", TestFiles.PNG);

    assertThrows(UnsupportedFileTypeException.class, () -> service.upload(file));

    verifyNoInteractions(storage, repository);
  }

  @Test
  void uploadRejectsExtensionThatDoesNotMatchContent() {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.pdf", "application/pdf", TestFiles.PNG);

    assertThrows(UnsupportedFileTypeException.class, () -> service.upload(file));
  }

  @Test
  void uploadRejectsFileWithoutExtension() {
    MockMultipartFile file = new MockMultipartFile("file", "photo", "image/png", TestFiles.PNG);

    assertThrows(UnsupportedFileTypeException.class, () -> service.upload(file));
  }

  @Test
  void uploadRejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    assertThrows(InvalidFileException.class, () -> service.upload(file));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "..", "../..", "folder/"})
  void uploadRejectsMissingOrTraversalOnlyNames(String name) {
    MockMultipartFile file = new MockMultipartFile("file", name, "image/png", TestFiles.PNG);

    assertThrows(InvalidFileException.class, () -> service.upload(file));

    verifyNoInteractions(storage, repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"../../etc/photo.png", "..\\..\\photo.png", "/abs/path/photo.png"})
  void uploadKeepsOnlyTheBaseNameOfAPathLikeName(String name) {
    MockMultipartFile file = new MockMultipartFile("file", name, "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    assertEquals("photo.png", response.originalName());
  }

  @Test
  void uploadRemovesStoredFileWhenRecordCannotBeSaved() {
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(repository.saveAndFlush(any(StoredFile.class)))
        .thenThrow(new IllegalStateException("db down"));

    assertThrows(IllegalStateException.class, () -> service.upload(file));

    verify(storage).delete("key-1");
  }

  @Test
  void listReturnsRecordsNewestFirst() {
    StoredFile stored = storedFile();
    when(repository.findAll(Sort.by(Sort.Direction.DESC, "uploadedAt", "id")))
        .thenReturn(List.of(stored));

    List<FileResponse> files = service.list();

    assertEquals(1, files.size());
    assertEquals("photo.png", files.get(0).originalName());
  }

  @Test
  void downloadReturnsContentWithOriginalName() {
    ByteArrayResource content = new ByteArrayResource(TestFiles.PNG);
    when(repository.findById(1L)).thenReturn(Optional.of(storedFile()));
    when(storage.load("key-1")).thenReturn(content);

    FileDownload download = service.download(1L);

    assertEquals("photo.png", download.originalName());
    assertEquals("image/png", download.contentType());
    assertEquals(content, download.content());
  }

  @Test
  void downloadOfUnknownFileFails() {
    when(repository.findById(9L)).thenReturn(Optional.empty());

    assertThrows(StoredFileNotFoundException.class, () -> service.download(9L));
  }

  @Test
  void deleteRemovesRecordAndFile() {
    StoredFile stored = storedFile();
    when(repository.findById(1L)).thenReturn(Optional.of(stored));

    service.delete(1L);

    verify(repository).delete(stored);
    verify(storage).delete("key-1");
  }

  @Test
  void deleteOfUnknownFileFailsWithoutTouchingStorage() {
    when(repository.findById(9L)).thenReturn(Optional.empty());

    assertThrows(StoredFileNotFoundException.class, () -> service.delete(9L));

    verify(storage, never()).delete(any());
  }

  @Test
  void uploadRejectsFileThatCannotBeRead() throws IOException {
    MultipartFile unreadable = mock(MultipartFile.class);
    when(unreadable.getOriginalFilename()).thenReturn("photo.png");
    when(unreadable.isEmpty()).thenReturn(false);
    when(unreadable.getSize()).thenReturn(64L);
    when(unreadable.getInputStream()).thenThrow(new IOException("disk error"));

    assertThrows(InvalidFileException.class, () -> service.upload(unreadable));

    verifyNoInteractions(storage, repository);
  }

  @Test
  void uploadStoresAThumbnailOfAnImage() {
    byte[] thumbnail = {1, 2, 3};
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(generator.create(FileType.PNG, TestFiles.PNG)).thenReturn(Optional.of(thumbnail));
    when(storage.storeBytes(thumbnail)).thenReturn("thumb-1");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    ArgumentCaptor<StoredFile> saved = ArgumentCaptor.forClass(StoredFile.class);
    verify(repository).saveAndFlush(saved.capture());
    assertTrue(response.thumbnailAvailable());
    assertEquals("thumb-1", saved.getValue().getThumbnailKey());
  }

  @Test
  void uploadOfAPdfCreatesNoThumbnail() {
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", TestFiles.PDF);
    when(storage.store(file)).thenReturn("key-1");
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    assertFalse(response.thumbnailAvailable());
    verifyNoInteractions(generator);
    verify(storage, never()).storeBytes(any());
  }

  @Test
  void uploadStillSucceedsWhenTheThumbnailCannotBeCreated() {
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(generator.create(FileType.PNG, TestFiles.PNG)).thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    assertFalse(response.thumbnailAvailable());
    verify(storage, never()).storeBytes(any());
  }

  @Test
  void uploadStillSucceedsWhenTheThumbnailCannotBeStored() {
    byte[] thumbnail = {1, 2, 3};
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(generator.create(FileType.PNG, TestFiles.PNG)).thenReturn(Optional.of(thumbnail));
    when(storage.storeBytes(thumbnail)).thenThrow(new FileStorageException("disk full"));
    when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(call -> call.getArgument(0));

    FileResponse response = service.upload(file);

    assertFalse(response.thumbnailAvailable());
    verify(storage, never()).delete(any());
  }

  @Test
  void uploadRemovesTheFileAndItsThumbnailWhenTheRecordCannotBeSaved() {
    byte[] thumbnail = {1, 2, 3};
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", TestFiles.PNG);
    when(storage.store(file)).thenReturn("key-1");
    when(generator.create(FileType.PNG, TestFiles.PNG)).thenReturn(Optional.of(thumbnail));
    when(storage.storeBytes(thumbnail)).thenReturn("thumb-1");
    when(repository.saveAndFlush(any(StoredFile.class)))
        .thenThrow(new IllegalStateException("db down"));

    assertThrows(IllegalStateException.class, () -> service.upload(file));

    verify(storage).delete("key-1");
    verify(storage).delete("thumb-1");
  }

  @Test
  void deleteRemovesTheThumbnailAsWell() {
    StoredFile stored =
        StoredFile.builder()
            .id(1L)
            .originalName("photo.png")
            .fileType(FileType.PNG)
            .sizeBytes(TestFiles.PNG.length)
            .storageKey("key-1")
            .thumbnailKey("thumb-1")
            .build();
    when(repository.findById(1L)).thenReturn(Optional.of(stored));

    service.delete(1L);

    verify(repository).delete(stored);
    verify(storage).delete("key-1");
    verify(storage).delete("thumb-1");
  }

  private StoredFile storedFile() {
    return StoredFile.builder()
        .id(1L)
        .originalName("photo.png")
        .fileType(FileType.PNG)
        .sizeBytes(TestFiles.PNG.length)
        .storageKey("key-1")
        .build();
  }
}
