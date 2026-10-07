package com.edstem.interviewprep.fileupload.dto.response;

import org.springframework.core.io.Resource;

public record ThumbnailDownload(String contentType, Resource content) {}
