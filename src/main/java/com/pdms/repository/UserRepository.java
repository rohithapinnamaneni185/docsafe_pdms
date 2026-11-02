package com.pdms.repository;

import com.pdms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR u.mobileNumber = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.id != :id")
    boolean existsByEmailIgnoreCaseAndIdNot(@Param("email") String email, @Param("id") Long id);

    List<User> findAllByOrderByCreatedAtAsc();

    @Query("SELECT u, COUNT(d) FROM User u LEFT JOIN Document d ON d.user = u AND d.isDeleted = false GROUP BY u ORDER BY u.createdAt ASC")
    List<Object[]> findAllUsersWithDocumentCount();

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.relationship) != 'self' AND u.role != 'ROLE_ADMIN'")
    long countNonSelfMembers();
}

