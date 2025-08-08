package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Goal;
import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class GoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category savingsCategory;
    private Category vacationCategory;
    private Goal goal1;
    private Goal goal2;
    private Goal goal3;

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


        savingsCategory = Category.builder()
                .name("Savings")
                .type(CategoryType.EXPENSE)
                .user(user1)
                .build();
        savingsCategory = categoryRepository.save(savingsCategory);

        vacationCategory = Category.builder()
                .name("Vacation")
                .type(CategoryType.EXPENSE)
                .user(user1)
                .build();
        vacationCategory = categoryRepository.save(vacationCategory);


        goal1 = Goal.builder()
                .user(user1)
                .category(savingsCategory)
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        goal2 = Goal.builder()
                .user(user1)
                .category(vacationCategory)
                .name("Europe Vacation")
                .targetAmount(new BigDecimal("5000.00"))
                .currentAmount(new BigDecimal("2000.00"))
                .targetDate(LocalDate.of(2024, 6, 30))
                .completed(false)
                .build();

        goal3 = Goal.builder()
                .user(user2)
                .category(savingsCategory)
                .name("House Down Payment")
                .targetAmount(new BigDecimal("50000.00"))
                .currentAmount(new BigDecimal("25000.00"))
                .targetDate(LocalDate.of(2025, 12, 31))
                .completed(false)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve goal")
    void testSaveAndRetrieveGoal() {
        Goal saved = goalRepository.save(goal1);
        
        Optional<Goal> retrieved = goalRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Emergency Fund");
        assertThat(retrieved.get().getTargetAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(retrieved.get().getCurrentAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find goals by user")
    void testFindByUserId() {
        goalRepository.save(goal1);
        goalRepository.save(goal2);
        goalRepository.save(goal3);

        List<Goal> user1Goals = goalRepository.findByUserId(user1.getId());
        List<Goal> user2Goals = goalRepository.findByUserId(user2.getId());

        assertThat(user1Goals).hasSize(2);
        assertThat(user2Goals).hasSize(1);
        
        assertThat(user1Goals).extracting("name")
                .containsExactlyInAnyOrder("Emergency Fund", "Europe Vacation");
    }

    @Test
    @DisplayName("Should find goals by target date range")
    void testFindGoalsByTargetDateRange() {
        goalRepository.save(goal1);
        goalRepository.save(goal2);
        goalRepository.save(goal3);

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        List<Goal> goalsInRange = goalRepository.findGoalsByTargetDateRange(
                user1.getId(), startDate, endDate);

        assertThat(goalsInRange).hasSize(2);
        assertThat(goalsInRange).extracting("name")
                .containsExactlyInAnyOrder("Emergency Fund", "Europe Vacation");
    }

    @Test
    @DisplayName("Should handle empty date range")
    void testEmptyDateRange() {
        goalRepository.save(goal1);
        goalRepository.save(goal2);

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        List<Goal> goalsInRange = goalRepository.findGoalsByTargetDateRange(
                user1.getId(), startDate, endDate);

        assertThat(goalsInRange).isEmpty();
    }

    @Test
    @DisplayName("Should update goal")
    void testUpdateGoal() {
        Goal saved = goalRepository.save(goal1);
        
        saved.setCurrentAmount(new BigDecimal("7500.00"));
        saved.setCompleted(true);
        Goal updated = goalRepository.save(saved);
        
        Optional<Goal> retrieved = goalRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCurrentAmount()).isEqualTo(new BigDecimal("7500.00"));
        assertThat(retrieved.get().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Should delete goal")
    void testDeleteGoal() {
        Goal saved = goalRepository.save(goal1);
        
        goalRepository.deleteById(saved.getId());
        
        Optional<Goal> retrieved = goalRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all goals")
    void testFindAllGoals() {
        goalRepository.save(goal1);
        goalRepository.save(goal2);
        goalRepository.save(goal3);

        List<Goal> allGoals = goalRepository.findAll();

        assertThat(allGoals).hasSize(3);
        assertThat(allGoals).extracting("name")
                .containsExactlyInAnyOrder("Emergency Fund", "Europe Vacation", "House Down Payment");
    }

    @Test
    @DisplayName("Should handle completed and incomplete goals")
    void testCompletedAndIncompleteGoals() {
        Goal completedGoal = Goal.builder()
                .user(user1)
                .category(savingsCategory)
                .name("Completed Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(true)
                .build();

        goalRepository.save(goal1);
        goalRepository.save(completedGoal);

        List<Goal> userGoals = goalRepository.findByUserId(user1.getId());

        assertThat(userGoals).hasSize(2);
        
        Goal completed = userGoals.stream()
                .filter(Goal::isCompleted)
                .findFirst()
                .orElse(null);
        
        Goal incomplete = userGoals.stream()
                .filter(goal -> !goal.isCompleted())
                .findFirst()
                .orElse(null);

        assertThat(completed).isNotNull();
        assertThat(completed.getName()).isEqualTo("Completed Goal");
        assertThat(incomplete).isNotNull();
        assertThat(incomplete.getName()).isEqualTo("Emergency Fund");
    }

    @Test
    @DisplayName("Should handle goal without category")
    void testGoalWithoutCategory() {
        Goal goalWithoutCategory = Goal.builder()
                .user(user1)
                .name("General Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .currentAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        Goal saved = goalRepository.save(goalWithoutCategory);
        
        Optional<Goal> retrieved = goalRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCategory()).isNull();
    }

    @Test
    @DisplayName("Should handle goals with different progress levels")
    void testGoalsWithDifferentProgress() {
        Goal lowProgressGoal = Goal.builder()
                .user(user1)
                .category(savingsCategory)
                .name("Low Progress")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        Goal highProgressGoal = Goal.builder()
                .user(user1)
                .category(savingsCategory)
                .name("High Progress")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("9000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        goalRepository.save(lowProgressGoal);
        goalRepository.save(highProgressGoal);

        List<Goal> userGoals = goalRepository.findByUserId(user1.getId());

        assertThat(userGoals).hasSize(2);
        
        Goal lowProgress = userGoals.stream()
                .filter(goal -> goal.getName().equals("Low Progress"))
                .findFirst()
                .orElse(null);
        
        Goal highProgress = userGoals.stream()
                .filter(goal -> goal.getName().equals("High Progress"))
                .findFirst()
                .orElse(null);

        assertThat(lowProgress).isNotNull();
        assertThat(lowProgress.getCurrentAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(highProgress).isNotNull();
        assertThat(highProgress.getCurrentAmount()).isEqualTo(new BigDecimal("9000.00"));
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Goal> userGoals = goalRepository.findByUserId(user1.getId());
        List<Goal> allGoals = goalRepository.findAll();

        assertThat(userGoals).isEmpty();
        assertThat(allGoals).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple goals with same target date")
    void testMultipleGoalsWithSameTargetDate() {
        Goal goal1 = Goal.builder()
                .user(user1)
                .category(savingsCategory)
                .name("Goal 1")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("500.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        Goal goal2 = Goal.builder()
                .user(user1)
                .category(vacationCategory)
                .name("Goal 2")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2024, 12, 31))
                .completed(false)
                .build();

        goalRepository.save(goal1);
        goalRepository.save(goal2);

        LocalDate startDate = LocalDate.of(2024, 12, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        List<Goal> goalsInRange = goalRepository.findGoalsByTargetDateRange(
                user1.getId(), startDate, endDate);

        assertThat(goalsInRange).hasSize(2);
        assertThat(goalsInRange).extracting("name")
                .containsExactlyInAnyOrder("Goal 1", "Goal 2");
    }
} 