package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.SourceType;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private Alert alert1;
    private Alert alert2;
    private Alert alert3;

    @BeforeEach
    void setup() {

        user1 = new User();
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        user1.setPassword("password123");
        user1.setCreatedAt(Instant.now());
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");
        user2.setPassword("password456");
        user2.setCreatedAt(Instant.now());
        user2 = userRepository.save(user2);


        alert1 = Alert.builder()
                .user(user1)
                .message("Budget limit exceeded for Groceries")
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        alert2 = Alert.builder()
                .user(user1)
                .message("Goal milestone reached: Emergency Fund")
                .sourceType(SourceType.GOAL)
                .sourceId(UUID.randomUUID())
                .read(true)
                .createdAt(Instant.now())
                .build();

        alert3 = Alert.builder()
                .user(user2)
                .message("Rule triggered: Dining spending limit")
                .sourceType(SourceType.RULE)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve alert")
    void testSaveAndRetrieveAlert() {
        Alert saved = alertRepository.save(alert1);
        
        Optional<Alert> retrieved = alertRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMessage()).isEqualTo("Budget limit exceeded for Groceries");
        assertThat(retrieved.get().getSourceType()).isEqualTo(SourceType.BUDGET);
        assertThat(retrieved.get().isRead()).isFalse();
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find alerts by user")
    void testFindByUserId() {
        alertRepository.save(alert1);
        alertRepository.save(alert2);
        alertRepository.save(alert3);

        List<Alert> user1Alerts = alertRepository.findByUserId(user1.getId());
        List<Alert> user2Alerts = alertRepository.findByUserId(user2.getId());

        assertThat(user1Alerts).hasSize(2);
        assertThat(user2Alerts).hasSize(1);
        
        assertThat(user1Alerts).extracting("message")
                .containsExactlyInAnyOrder("Budget limit exceeded for Groceries", "Goal milestone reached: Emergency Fund");
    }

    @Test
    @DisplayName("Should update alert")
    void testUpdateAlert() {
        Alert saved = alertRepository.save(alert1);
        
        saved.setRead(true);
        saved.setMessage("Updated alert message");
        Alert updated = alertRepository.save(saved);
        
        Optional<Alert> retrieved = alertRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().isRead()).isTrue();
        assertThat(retrieved.get().getMessage()).isEqualTo("Updated alert message");
    }

    @Test
    @DisplayName("Should delete alert")
    void testDeleteAlert() {
        Alert saved = alertRepository.save(alert1);
        
        alertRepository.deleteById(saved.getId());
        
        Optional<Alert> retrieved = alertRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all alerts")
    void testFindAllAlerts() {
        alertRepository.save(alert1);
        alertRepository.save(alert2);
        alertRepository.save(alert3);

        List<Alert> allAlerts = alertRepository.findAll();

        assertThat(allAlerts).hasSize(3);
        assertThat(allAlerts).extracting("message")
                .containsExactlyInAnyOrder("Budget limit exceeded for Groceries", "Goal milestone reached: Emergency Fund", "Rule triggered: Dining spending limit");
    }

    @Test
    @DisplayName("Should handle alerts with different source types")
    void testAlertsWithDifferentSourceTypes() {
        Alert budgetAlert = Alert.builder()
                .user(user1)
                .message("Budget Alert")
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Alert goalAlert = Alert.builder()
                .user(user1)
                .message("Goal Alert")
                .sourceType(SourceType.GOAL)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Alert ruleAlert = Alert.builder()
                .user(user1)
                .message("Rule Alert")
                .sourceType(SourceType.RULE)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(budgetAlert);
        alertRepository.save(goalAlert);
        alertRepository.save(ruleAlert);

        List<Alert> userAlerts = alertRepository.findByUserId(user1.getId());

        assertThat(userAlerts).hasSize(3);
        assertThat(userAlerts).extracting("sourceType")
                .containsExactlyInAnyOrder(SourceType.BUDGET, SourceType.GOAL, SourceType.RULE);
    }

    @Test
    @DisplayName("Should handle read and unread alerts")
    void testReadAndUnreadAlerts() {
        Alert unreadAlert = Alert.builder()
                .user(user1)
                .message("Unread Alert")
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Alert readAlert = Alert.builder()
                .user(user1)
                .message("Read Alert")
                .sourceType(SourceType.GOAL)
                .sourceId(UUID.randomUUID())
                .read(true)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(unreadAlert);
        alertRepository.save(readAlert);

        List<Alert> userAlerts = alertRepository.findByUserId(user1.getId());

        assertThat(userAlerts).hasSize(2);
        
        Alert unread = userAlerts.stream()
                .filter(alert -> !alert.isRead())
                .findFirst()
                .orElse(null);
        
        Alert read = userAlerts.stream()
                .filter(Alert::isRead)
                .findFirst()
                .orElse(null);

        assertThat(unread).isNotNull();
        assertThat(unread.getMessage()).isEqualTo("Unread Alert");
        assertThat(read).isNotNull();
        assertThat(read.getMessage()).isEqualTo("Read Alert");
    }

    @Test
    @DisplayName("Should handle alerts with different creation times")
    void testAlertsWithDifferentCreationTimes() {
        Alert oldAlert = Alert.builder()
                .user(user1)
                .message("Old Alert")
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now().minusSeconds(3600)) // 1 hour ago
                .build();

        Alert newAlert = Alert.builder()
                .user(user1)
                .message("New Alert")
                .sourceType(SourceType.GOAL)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(oldAlert);
        alertRepository.save(newAlert);

        List<Alert> userAlerts = alertRepository.findByUserId(user1.getId());

        assertThat(userAlerts).hasSize(2);
        assertThat(userAlerts).extracting("message")
                .containsExactlyInAnyOrder("Old Alert", "New Alert");
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Alert> userAlerts = alertRepository.findByUserId(user1.getId());
        List<Alert> allAlerts = alertRepository.findAll();

        assertThat(userAlerts).isEmpty();
        assertThat(allAlerts).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple alerts for same user")
    void testMultipleAlertsForSameUser() {
        Alert alert1 = Alert.builder()
                .user(user1)
                .message("Alert 1")
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Alert alert2 = Alert.builder()
                .user(user1)
                .message("Alert 2")
                .sourceType(SourceType.GOAL)
                .sourceId(UUID.randomUUID())
                .read(true)
                .createdAt(Instant.now())
                .build();

        Alert alert3 = Alert.builder()
                .user(user1)
                .message("Alert 3")
                .sourceType(SourceType.RULE)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);
        alertRepository.save(alert3);

        List<Alert> userAlerts = alertRepository.findByUserId(user1.getId());

        assertThat(userAlerts).hasSize(3);
        assertThat(userAlerts).extracting("message")
                .containsExactlyInAnyOrder("Alert 1", "Alert 2", "Alert 3");
    }

    @Test
    @DisplayName("Should handle alerts with long messages")
    void testAlertsWithLongMessages() {
        String longMessage = "This is a very long alert message that contains detailed information about the budget limit being exceeded. " +
                "The user has spent more than the allocated amount for the groceries category in the current month. " +
                "This alert should be displayed properly even with such a long message.";

        Alert longMessageAlert = Alert.builder()
                .user(user1)
                .message(longMessage)
                .sourceType(SourceType.BUDGET)
                .sourceId(UUID.randomUUID())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Alert saved = alertRepository.save(longMessageAlert);
        
        Optional<Alert> retrieved = alertRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMessage()).isEqualTo(longMessage);
    }
} 