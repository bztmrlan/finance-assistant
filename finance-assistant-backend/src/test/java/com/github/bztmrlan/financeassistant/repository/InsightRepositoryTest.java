package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.InsightType;
import com.github.bztmrlan.financeassistant.model.Insight;
import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class InsightRepositoryTest {

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private Insight insight1;
    private Insight insight2;
    private Insight insight3;

    @BeforeEach
    void setup() {
        // Create users
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


        insight1 = Insight.builder()
                .user(user1)
                .message("Your spending on groceries has increased by 15% compared to last month")
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        insight2 = Insight.builder()
                .user(user1)
                .message("You could save $200/month by reducing dining out expenses")
                .type(InsightType.SAVINGS_OPPORTUNITY)
                .viewed(true)
                .generatedAt(Instant.now())
                .build();

        insight3 = Insight.builder()
                .user(user2)
                .message("Unusual activity detected: Large transaction in entertainment category")
                .type(InsightType.UNUSUAL_ACTIVITY)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve insight")
    void testSaveAndRetrieveInsight() {
        Insight saved = insightRepository.save(insight1);
        
        Optional<Insight> retrieved = insightRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMessage()).isEqualTo("Your spending on groceries has increased by 15% compared to last month");
        assertThat(retrieved.get().getType()).isEqualTo(InsightType.SPENDING_TREND);
        assertThat(retrieved.get().isViewed()).isFalse();
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find insights by user")
    void testFindByUserId() {
        insightRepository.save(insight1);
        insightRepository.save(insight2);
        insightRepository.save(insight3);

        List<Insight> user1Insights = insightRepository.findByUserId(user1.getId());
        List<Insight> user2Insights = insightRepository.findByUserId(user2.getId());

        assertThat(user1Insights).hasSize(2);
        assertThat(user2Insights).hasSize(1);
        
        assertThat(user1Insights).extracting("message")
                .containsExactlyInAnyOrder("Your spending on groceries has increased by 15% compared to last month", 
                        "You could save $200/month by reducing dining out expenses");
    }

    @Test
    @DisplayName("Should update insight")
    void testUpdateInsight() {
        Insight saved = insightRepository.save(insight1);
        
        saved.setViewed(true);
        saved.setMessage("Updated insight message");
        Insight updated = insightRepository.save(saved);
        
        Optional<Insight> retrieved = insightRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().isViewed()).isTrue();
        assertThat(retrieved.get().getMessage()).isEqualTo("Updated insight message");
    }

    @Test
    @DisplayName("Should delete insight")
    void testDeleteInsight() {
        Insight saved = insightRepository.save(insight1);
        
        insightRepository.deleteById(saved.getId());
        
        Optional<Insight> retrieved = insightRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all insights")
    void testFindAllInsights() {
        insightRepository.save(insight1);
        insightRepository.save(insight2);
        insightRepository.save(insight3);

        List<Insight> allInsights = insightRepository.findAll();

        assertThat(allInsights).hasSize(3);
        assertThat(allInsights).extracting("message")
                .containsExactlyInAnyOrder("Your spending on groceries has increased by 15% compared to last month", 
                        "You could save $200/month by reducing dining out expenses",
                        "Unusual activity detected: Large transaction in entertainment category");
    }

    @Test
    @DisplayName("Should handle insights with different types")
    void testInsightsWithDifferentTypes() {
        Insight spendingTrendInsight = Insight.builder()
                .user(user1)
                .message("Spending Trend Insight")
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight savingOpportunityInsight = Insight.builder()
                .user(user1)
                .message("Saving Opportunity Insight")
                .type(InsightType.SAVINGS_OPPORTUNITY)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight unusualActivityInsight = Insight.builder()
                .user(user1)
                .message("Unusual Activity Insight")
                .type(InsightType.UNUSUAL_ACTIVITY)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight budgetReviewInsight = Insight.builder()
                .user(user1)
                .message("Budget Review Insight")
                .type(InsightType.BUDGET_REVIEW)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        insightRepository.save(spendingTrendInsight);
        insightRepository.save(savingOpportunityInsight);
        insightRepository.save(unusualActivityInsight);
        insightRepository.save(budgetReviewInsight);

        List<Insight> userInsights = insightRepository.findByUserId(user1.getId());

        assertThat(userInsights).hasSize(4);
        assertThat(userInsights).extracting("type")
                .containsExactlyInAnyOrder(InsightType.SPENDING_TREND, InsightType.SAVINGS_OPPORTUNITY, 
                        InsightType.UNUSUAL_ACTIVITY, InsightType.BUDGET_REVIEW);
    }

    @Test
    @DisplayName("Should handle viewed and unviewed insights")
    void testViewedAndUnviewedInsights() {
        Insight unviewedInsight = Insight.builder()
                .user(user1)
                .message("Unviewed Insight")
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight viewedInsight = Insight.builder()
                .user(user1)
                .message("Viewed Insight")
                .type(InsightType.SAVINGS_OPPORTUNITY)
                .viewed(true)
                .generatedAt(Instant.now())
                .build();

        insightRepository.save(unviewedInsight);
        insightRepository.save(viewedInsight);

        List<Insight> userInsights = insightRepository.findByUserId(user1.getId());

        assertThat(userInsights).hasSize(2);
        
        Insight unviewed = userInsights.stream()
                .filter(insight -> !insight.isViewed())
                .findFirst()
                .orElse(null);
        
        Insight viewed = userInsights.stream()
                .filter(Insight::isViewed)
                .findFirst()
                .orElse(null);

        assertThat(unviewed).isNotNull();
        assertThat(unviewed.getMessage()).isEqualTo("Unviewed Insight");
        assertThat(viewed).isNotNull();
        assertThat(viewed.getMessage()).isEqualTo("Viewed Insight");
    }

    @Test
    @DisplayName("Should handle insights with different generation times")
    void testInsightsWithDifferentGenerationTimes() {
        Insight oldInsight = Insight.builder()
                .user(user1)
                .message("Old Insight")
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now().minusSeconds(3600)) // 1 hour ago
                .build();

        Insight newInsight = Insight.builder()
                .user(user1)
                .message("New Insight")
                .type(InsightType.SAVINGS_OPPORTUNITY)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        insightRepository.save(oldInsight);
        insightRepository.save(newInsight);

        List<Insight> userInsights = insightRepository.findByUserId(user1.getId());

        assertThat(userInsights).hasSize(2);
        assertThat(userInsights).extracting("message")
                .containsExactlyInAnyOrder("Old Insight", "New Insight");
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Insight> userInsights = insightRepository.findByUserId(user1.getId());
        List<Insight> allInsights = insightRepository.findAll();

        assertThat(userInsights).isEmpty();
        assertThat(allInsights).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple insights for same user")
    void testMultipleInsightsForSameUser() {
        Insight insight1 = Insight.builder()
                .user(user1)
                .message("Insight 1")
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight insight2 = Insight.builder()
                .user(user1)
                .message("Insight 2")
                .type(InsightType.SAVINGS_OPPORTUNITY)
                .viewed(true)
                .generatedAt(Instant.now())
                .build();

        Insight insight3 = Insight.builder()
                .user(user1)
                .message("Insight 3")
                .type(InsightType.UNUSUAL_ACTIVITY)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        insightRepository.save(insight1);
        insightRepository.save(insight2);
        insightRepository.save(insight3);

        List<Insight> userInsights = insightRepository.findByUserId(user1.getId());

        assertThat(userInsights).hasSize(3);
        assertThat(userInsights).extracting("message")
                .containsExactlyInAnyOrder("Insight 1", "Insight 2", "Insight 3");
    }

    @Test
    @DisplayName("Should handle insights with long messages")
    void testInsightsWithLongMessages() {
        String longMessage = "This is a very detailed insight message that provides comprehensive analysis of the user's spending patterns. " +
                "It includes specific recommendations for improving financial health, such as reducing discretionary spending " +
                "and increasing savings contributions. The insight also highlights trends and patterns that may not be immediately obvious.";

        Insight longMessageInsight = Insight.builder()
                .user(user1)
                .message(longMessage)
                .type(InsightType.BUDGET_REVIEW)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight saved = insightRepository.save(longMessageInsight);
        
        Optional<Insight> retrieved = insightRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMessage()).isEqualTo(longMessage);
    }

    @Test
    @DisplayName("Should handle insights with special characters in messages")
    void testInsightsWithSpecialCharacters() {
        String specialMessage = "Insight with special characters: $100.50, 15%, €200, ¥1000, and symbols like @#$%^&*()";
        
        Insight specialCharInsight = Insight.builder()
                .user(user1)
                .message(specialMessage)
                .type(InsightType.SPENDING_TREND)
                .viewed(false)
                .generatedAt(Instant.now())
                .build();

        Insight saved = insightRepository.save(specialCharInsight);
        
        Optional<Insight> retrieved = insightRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMessage()).isEqualTo(specialMessage);
    }
} 