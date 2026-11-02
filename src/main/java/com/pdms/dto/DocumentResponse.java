package com.pdms.dto;

import com.pdms.entity.Document;
import com.pdms.model.ExpiryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a document returned to the frontend.
 * Enriched with calculated expiry status and family member ownership info.
 */
public class DocumentResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userRelationship;
    private String documentName;
    private String category;
    private String issuingOrganization;
    private String referenceNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String notes;
    private String imagePath;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String imageUrl;
    private ExpiryStatus status;
    private String statusLabel;
    private String statusCssClass;
    private Long daysRemaining;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {
    }

    public static DocumentResponse fromEntity(Document doc) {
        if (doc == null) {
            return null;
        }

        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());

        if (doc.getUser() != null) {
            resp.setUserId(doc.getUser().getId());
            resp.setUserName(doc.getUser().getFullName());
            resp.setUserRelationship(doc.getUser().getRelationship());
        }

        resp.setDocumentName(doc.getDocumentName());
        resp.setCategory(doc.getCategory());
        resp.setIssuingOrganization(doc.getIssuingOrganization());
        resp.setReferenceNumber(doc.getReferenceNumber());
        resp.setIssueDate(doc.getIssueDate());
        resp.setExpiryDate(doc.getExpiryDate());
        resp.setNotes(doc.getNotes());
        resp.setImagePath(doc.getImagePath());
        resp.setFileName(doc.getFileName());
        resp.setFileSize(doc.getFileSize());
        resp.setFileType(doc.getFileType());

        if (doc.getFileName() != null && !doc.getFileName().isEmpty()) {
            resp.setImageUrl("/api/documents/images/" + doc.getFileName());
        } else {
            resp.setImageUrl(null);
        }

        // Authoritative backend expiry status calculation
        ExpiryStatus calculatedStatus = ExpiryStatus.calculate(doc.getExpiryDate());
        resp.setStatus(calculatedStatus);
        resp.setStatusLabel(calculatedStatus.getLabel());
        resp.setStatusCssClass(calculatedStatus.getCssClass());
        resp.setDaysRemaining(ExpiryStatus.calculateDaysRemaining(doc.getExpiryDate()));

        resp.setCreatedAt(doc.getCreatedAt());
        resp.setUpdatedAt(doc.getUpdatedAt());
        resp.setDeleted(doc.isDeleted());
        resp.setDeletedAt(doc.getDeletedAt());

        return resp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRelationship() {
        return userRelationship;
    }

    public void setUserRelationship(String userRelationship) {
        this.userRelationship = userRelationship;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIssuingOrganization() {
        return issuingOrganization;
    }

    public void setIssuingOrganization(String issuingOrganization) {
        this.issuingOrganization = issuingOrganization;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ExpiryStatus getStatus() {
        return status;
    }

    public void setStatus(ExpiryStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusCssClass() {
        return statusCssClass;
    }

    public void setStatusCssClass(String statusCssClass) {
        this.statusCssClass = statusCssClass;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
