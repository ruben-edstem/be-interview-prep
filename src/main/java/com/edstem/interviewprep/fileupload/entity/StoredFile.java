package com.edstem.interviewprep.fileupload.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "stored_files")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StoredFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String originalName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FileType fileType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false, unique = true, updatable = false)
  private String storageKey;

  @Column(unique = true, updatable = false)
  private String thumbnailKey;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant uploadedAt;
}
