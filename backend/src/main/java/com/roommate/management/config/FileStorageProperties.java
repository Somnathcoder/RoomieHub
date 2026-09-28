package com.roommate.management.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    private String baseDir;
    private long maxFileSizeMb;
    private List<String> allowedImageTypes;
    private List<String> allowedDocTypes;
}
