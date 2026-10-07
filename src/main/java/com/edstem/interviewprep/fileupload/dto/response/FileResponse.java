package com.edstem.interviewprep.fileupload.dto.response;

import com.edstem.interviewprep.fileupload.entity.StoredFile;
import java.time.Instant;

public record FileResponse(
    Long id, String originalName, String contentType, long size, Instant uploadedAt) {

  public static FileResponse from(StoredFile file) {
    return new FileResponse(
        file.getId(),
        file.getOriginalName(),
        file.getFileType().getMediaType(),
        file.getSizeBytes(),
        file.getUploadedAt());
  }
}
