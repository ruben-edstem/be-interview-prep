package com.edstem.interviewprep.fileupload.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.edstem.interviewprep.fileupload.entity.FileType;
import com.edstem.interviewprep.fileupload.entity.StoredFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class StoredFileRepositoryTest {

  @Autowired private StoredFileRepository repository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savePersistsMetadataAndSetsUploadTime() {
    StoredFile file =
        StoredFile.builder()
            .originalName("photo.png")
            .fileType(FileType.PNG)
            .sizeBytes(64)
            .storageKey("key-1")
            .build();

    StoredFile saved = repository.saveAndFlush(file);
    entityManager.clear();
    StoredFile found = repository.findById(saved.getId()).orElseThrow();

    assertEquals("photo.png", found.getOriginalName());
    assertEquals(FileType.PNG, found.getFileType());
    assertEquals(64, found.getSizeBytes());
    assertNotNull(found.getUploadedAt());
    assertNull(found.getThumbnailKey());
  }

  @Test
  void saveKeepsTheThumbnailKey() {
    StoredFile file =
        StoredFile.builder()
            .originalName("photo.png")
            .fileType(FileType.PNG)
            .sizeBytes(64)
            .storageKey("key-1")
            .thumbnailKey("thumb-1")
            .build();

    StoredFile saved = repository.saveAndFlush(file);
    entityManager.clear();
    StoredFile found = repository.findById(saved.getId()).orElseThrow();

    assertEquals("thumb-1", found.getThumbnailKey());
  }
}
