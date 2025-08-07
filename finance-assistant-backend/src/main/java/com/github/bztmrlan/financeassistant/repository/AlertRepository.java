package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByUserId(UUID userId);
}
