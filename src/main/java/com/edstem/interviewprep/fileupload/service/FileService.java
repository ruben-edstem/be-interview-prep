package com.edstem.interviewprep.fileupload.service;

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
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

  private static final int MAX_NAME_LENGTH = 255;
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "uploadedAt", "id");

  private final StoredFileRepository repository;
  private final FileStorageService storage;
  private final FileUploadProperties properties;
  private final ThumbnailGenerator generator;

  @Transactional
  public FileResponse upload(MultipartFile file) {
    String originalName = cleanName(file.getOriginalFilename());
    validateSize(file);
    FileType type = detectType(file);
    validateExtension(originalName, type);

    String storageKey = storage.store(file);
    String thumbnailKey = null;
    try {
      thumbnailKey = storeThumbnail(file, type);
      StoredFile saved =
          repository.saveAndFlush(
              StoredFile.builder()
                  .originalName(originalName)
                  .fileType(type)
                  .sizeBytes(file.getSize())
                  .storageKey(storageKey)
                  .thumbnailKey(thumbnailKey)
                  .build());
      log.info("Stored file {} as {} ({} bytes)", saved.getId(), type, saved.getSizeBytes());
      return FileResponse.from(saved);
    } catch (RuntimeException e) {
      storage.delete(storageKey);
      if (thumbnailKey != null) {
        storage.delete(thumbnailKey);
      }
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public List<FileResponse> list() {
    return repository.findAll(NEWEST_FIRST).stream().map(FileResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public FileDownload download(Long id) {
    StoredFile file = find(id);
    return new FileDownload(
        file.getOriginalName(),
        file.getFileType().getMediaType(),
        file.getSizeBytes(),
        storage.load(file.getStorageKey()));
  }

  @Transactional
  public void delete(Long id) {
    StoredFile file = find(id);
    repository.delete(file);
    if (file.getThumbnailKey() != null) {
      storage.delete(file.getThumbnailKey());
    }
    storage.delete(file.getStorageKey());
    log.info("Deleted file {}", id);
  }

  private String storeThumbnail(MultipartFile file, FileType type) {
    if (!type.isImage()) {
      return null;
    }
    try {
      return generator.create(type, file.getBytes()).map(storage::storeBytes).orElse(null);
    } catch (IOException | FileStorageException e) {
      log.warn("Stored the file without a thumbnail", e);
      return null;
    }
  }

  private StoredFile find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new StoredFileNotFoundException("File " + id + " was not found"));
  }

  private String cleanName(String rawName) {
    if (rawName == null) {
      throw new InvalidFileException("A file name is required");
    }
    int lastSeparator = Math.max(rawName.lastIndexOf('/'), rawName.lastIndexOf('\\'));
    String name =
        rawName
            .substring(lastSeparator + 1)
            .chars()
            .filter(character -> !Character.isISOControl(character))
            .collect(
                StringBuilder::new,
                (builder, character) -> builder.appendCodePoint(character),
                StringBuilder::append)
            .toString()
            .strip();
    if (name.isEmpty() || name.equals(".") || name.equals("..")) {
      throw new InvalidFileException("A valid file name is required");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new InvalidFileException(
          "File name must be at most " + MAX_NAME_LENGTH + " characters");
    }
    return name;
  }

  private void validateSize(MultipartFile file) {
    long limit = properties.maxSize().toBytes();
    if (file.isEmpty()) {
      throw new InvalidFileException("The file is empty");
    }
    if (file.getSize() > limit) {
      throw new FileTooLargeException(
          "File is " + file.getSize() + " bytes; the limit is " + limit + " bytes");
    }
  }

  private FileType detectType(MultipartFile file) {
    try (InputStream content = file.getInputStream()) {
      byte[] header = content.readNBytes(FileType.MAX_SIGNATURE_LENGTH);
      return FileType.detect(header)
          .orElseThrow(
              () ->
                  new UnsupportedFileTypeException(
                      "Only JPEG, PNG and PDF files are allowed, and the content must match"));
    } catch (IOException e) {
      throw new InvalidFileException("The file could not be read");
    }
  }

  private void validateExtension(String name, FileType type) {
    int dot = name.lastIndexOf('.');
    String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!type.allowsExtension(extension)) {
      throw new UnsupportedFileTypeException(
          "The file extension does not match its " + type + " content");
    }
  }
}
