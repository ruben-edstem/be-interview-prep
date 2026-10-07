package com.edstem.interviewprep.fileupload.service;

import com.edstem.interviewprep.fileupload.config.FileUploadProperties;
import com.edstem.interviewprep.fileupload.exception.FileStorageException;
import com.edstem.interviewprep.fileupload.exception.StoredFileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private final Path root;

  public FileStorageService(FileUploadProperties properties) {
    this.root = properties.directory().toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new FileStorageException("Could not create the storage directory", e);
    }
  }

  public String store(MultipartFile file) {
    String key = UUID.randomUUID().toString();
    Path target = resolve(key);
    try (InputStream content = file.getInputStream()) {
      Files.copy(content, target);
    } catch (IOException e) {
      delete(key);
      throw new FileStorageException("Could not store the file", e);
    }
    return key;
  }

  public Resource load(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) {
      throw new StoredFileNotFoundException("The stored file content is missing");
    }
    return new FileSystemResource(path);
  }

  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new FileStorageException("Could not delete the file", e);
    }
  }

  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root) || path.equals(root)) {
      throw new FileStorageException("Invalid storage key");
    }
    return path;
  }
}
