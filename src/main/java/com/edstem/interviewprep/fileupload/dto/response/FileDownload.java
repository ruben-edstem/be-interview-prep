package com.edstem.interviewprep.fileupload.dto.response;

import org.springframework.core.io.Resource;

public record FileDownload(String originalName, String contentType, long size, Resource content) {}
