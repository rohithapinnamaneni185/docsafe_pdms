package com.pdms.repository;

import com.pdms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT d FROM Document d WHERE " +
           "d.isDeleted = false AND " +
           "(:userId IS NULL OR d.user.id = :userId) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(d.category) = LOWER(:category)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(d.documentName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.issuingOrganization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.notes) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Document> searchDocuments(@Param("query") String query, 
                                   @Param("category") String category, 
                                   @Param("userId") Long userId);

    List<Document> findByIsDeletedFalse();

    List<Document> findByUserIdAndIsDeletedFalse(Long userId);

    long countByIsDeletedFalse();

    long countByUserIdAndIsDeletedFalse(Long userId);

    long countByIsDeletedTrue();

    long countByUserIdAndIsDeletedTrue(Long userId);

    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.isDeleted = false AND (:userId IS NULL OR d.user.id = :userId) GROUP BY d.category")
    List<Object[]> countDocumentsByCategory(@Param("userId") Long userId);

    List<Document> findTop5ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    List<Document> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    List<Document> findTop5ByUserIdAndIsDeletedFalseAndExpiryDateBetweenOrderByExpiryDateAsc(Long userId, LocalDate start, LocalDate end);

    List<Document> findTop5ByIsDeletedFalseAndExpiryDateBetweenOrderByExpiryDateAsc(LocalDate start, LocalDate end);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = true AND (:userId IS NULL OR d.user.id = :userId) ORDER BY d.deletedAt DESC")
    List<Document> findTrashDocuments(@Param("userId") Long userId);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.expiryDate IS NOT NULL AND (:userId IS NULL OR d.user.id = :userId) ORDER BY d.expiryDate ASC")
    List<Document> findCalendarDocuments(@Param("userId") Long userId);
}
