package com.edstem.interviewprep.fileupload.entity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;

@Getter
public enum FileType {
  JPEG(
      "image/jpeg",
      Set.of("jpg", "jpeg"),
      new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
      "jpeg"),
  PNG(
      "image/png",
      Set.of("png"),
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
      "png"),
  PDF("application/pdf", Set.of("pdf"), "%PDF-".getBytes(StandardCharsets.US_ASCII), null);

  public static final int MAX_SIGNATURE_LENGTH = 8;

  private final String mediaType;
  private final Set<String> extensions;
  private final byte[] signature;
  private final String imageFormat;

  FileType(String mediaType, Set<String> extensions, byte[] signature, String imageFormat) {
    this.mediaType = mediaType;
    this.extensions = extensions;
    this.signature = signature;
    this.imageFormat = imageFormat;
  }

  public static Optional<FileType> detect(byte[] header) {
    return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
  }

  public boolean allowsExtension(String extension) {
    return extensions.contains(extension);
  }

  public boolean isImage() {
    return imageFormat != null;
  }

  private boolean matches(byte[] header) {
    return header.length >= signature.length
        && Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
  }
}
