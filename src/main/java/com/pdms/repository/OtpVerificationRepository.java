package com.pdms.repository;

import com.pdms.entity.OtpVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findByVerificationToken(String verificationToken);

    List<OtpVerification> findAllByEmailIgnoreCaseAndUsedFalse(String email);

    void deleteByEmailIgnoreCase(String email);
}
