package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
    UserSettings findByUserId(UUID userId);
}