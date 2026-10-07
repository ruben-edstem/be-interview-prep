package com.edstem.interviewprep.fileupload.repository;

import com.edstem.interviewprep.fileupload.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {}
