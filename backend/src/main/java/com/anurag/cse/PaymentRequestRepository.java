package com.anurag.cse;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    List<PaymentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}