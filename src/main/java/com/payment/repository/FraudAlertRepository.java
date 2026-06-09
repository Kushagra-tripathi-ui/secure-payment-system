package com.payment.repository;

import com.payment.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(String status);
}
