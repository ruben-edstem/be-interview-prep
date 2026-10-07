package com.edstem.interviewprep.fileupload.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.file-upload")
public record FileUploadProperties(
    @DefaultValue("uploads") Path directory, @DefaultValue("5MB") DataSize maxSize) {}
