package com.roommate.management.service;

import com.roommate.management.config.FileStorageProperties;
import com.roommate.management.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties properties;

    public String store(MultipartFile file, String subFolder, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > properties.getMaxFileSizeMb() * 1024L * 1024L) {
            throw new BadRequestException("File exceeds the maximum allowed size of " + properties.getMaxFileSizeMb() + "MB");
        }
        String contentType = file.getContentType();
        if (allowedTypes != null && !allowedTypes.isEmpty() && (contentType == null || !allowedTypes.contains(contentType))) {
            throw new BadRequestException("Unsupported file type: " + contentType + ". Allowed: " + allowedTypes);
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot);
        }
        // Safe, unpredictable filename - never trust the client-supplied name directly
        String safeFileName = UUID.randomUUID() + extension.replaceAll("[^a-zA-Z0-9.]", "");

        try {
            Path targetDir = Paths.get(properties.getBaseDir(), subFolder).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(safeFileName).normalize();
            if (!targetPath.startsWith(targetDir)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return subFolder + "/" + safeFileName;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    public String toPublicUrl(String relativePath) {
        if (relativePath == null) return null;
        return "/uploads/" + relativePath;
    }

    public Path resolve(String relativePath) {
        return Paths.get(properties.getBaseDir(), relativePath).toAbsolutePath().normalize();
    }
}
