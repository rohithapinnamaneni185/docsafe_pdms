package com.pdms.service;

import com.pdms.exception.BadRequestException;
import com.pdms.exception.FileStorageException;
import com.pdms.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service handling secure file storage, validation, retrieval, and deletion.
 */
@Service
public class FileStorageService {

    private final Path uploadLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );

    public record StoredFileInfo(String fileName, String relativePath, long fileSize, String contentType) {}

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory: " + this.uploadLocation, e);
        }
    }

    /**
     * Validates and saves an uploaded image file securely.
     * Generates a unique UUID-based filename.
     */
    public StoredFileInfo storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.png"
        );

        // Security check against path traversal
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Filename contains invalid path characters: " + originalFilename);
        }

        // Validate extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file format. Only JPG, JPEG, and PNG images are supported.");
        }

        // Validate content type if present
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type: " + contentType + ". Supported types are image/jpeg and image/png.");
        }

        // Generate a cryptographically unique filename
        String storedFilename = UUID.randomUUID() + extension;
        Path targetPath = this.uploadLocation.resolve(storedFilename).normalize();

        // Extra check to verify target is strictly inside upload directory
        if (!targetPath.startsWith(this.uploadLocation)) {
            throw new FileStorageException("Cannot store file outside current directory");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = "uploads/" + storedFilename;
            return new StoredFileInfo(storedFilename, relativePath, file.getSize(), 
                    contentType != null ? contentType : "image/" + extension.replace(".", ""));
        } catch (IOException e) {
            throw new FileStorageException("Failed to save file: " + storedFilename, e);
        }
    }

    /**
     * Loads a file as a Spring Resource for image viewing and downloads.
     */
    public Resource loadFileAsResource(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResourceNotFoundException("File name was not specified");
        }

        try {
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadLocation)) {
                throw new BadRequestException("Access denied: Invalid file path request");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on server: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + fileName);
        }
    }

    /**
     * Safely deletes an existing file from the filesystem.
     */
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        try {
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            if (filePath.startsWith(this.uploadLocation)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log and allow operation to proceed without throwing fatal error
            System.err.println("Warning: Unable to delete file " + fileName + ": " + e.getMessage());
        }
        return false;
    }

    public Path getUploadLocation() {
        return uploadLocation;
    }
}
