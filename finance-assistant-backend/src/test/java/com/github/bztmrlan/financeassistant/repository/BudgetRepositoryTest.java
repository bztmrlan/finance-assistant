package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.BudgetCategory;
import com.github.bztmrlan.financeassistant.model.Category;
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
public class BudgetRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    private User user1;
    private User user2;
    private Category groceriesCategory;
    private Category diningCategory;
    private Budget budget1;
    private Budget budget2;
    private BudgetCategory budgetCategory1;
    private BudgetCategory budgetCategory2;

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


        groceriesCategory = Category.builder()
                .name("Groceries")
                .type(com.github.bztmrlan.financeassistant.enums.CategoryType.EXPENSE)
                .user(user1)
                .build();
        groceriesCategory = categoryRepository.save(groceriesCategory);

        diningCategory = Category.builder()
                .name("Dining")
                .type(com.github.bztmrlan.financeassistant.enums.CategoryType.EXPENSE)
                .user(user1)
                .build();
        diningCategory = categoryRepository.save(diningCategory);


        budget1 = Budget.builder()
                .user(user1)
                .name("January Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(BudgetStatus.ACTIVE)
                .build();

        budget2 = Budget.builder()
                .user(user2)
                .name("February Budget")
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 2, 29))
                .status(BudgetStatus.UPCOMING)
                .build();


        budgetCategory1 = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("350.00"))
                .build();

        budgetCategory2 = BudgetCategory.builder()
                .budget(budget1)
                .category(diningCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(new BigDecimal("180.00"))
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve budget")
    void testSaveAndRetrieveBudget() {
        Budget saved = budgetRepository.save(budget1);
        
        Optional<Budget> retrieved = budgetRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("January Budget");
        assertThat(retrieved.get().getStatus()).isEqualTo(BudgetStatus.ACTIVE);
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find budgets by user")
    void testFindByUserId() {
        budgetRepository.save(budget1);
        budgetRepository.save(budget2);

        List<Budget> user1Budgets = budgetRepository.findByUserId(user1.getId());
        List<Budget> user2Budgets = budgetRepository.findByUserId(user2.getId());

        assertThat(user1Budgets).hasSize(1);
        assertThat(user2Budgets).hasSize(1);
        
        assertThat(user1Budgets.get(0).getName()).isEqualTo("January Budget");
        assertThat(user2Budgets.get(0).getName()).isEqualTo("February Budget");
    }

    @Test
    @DisplayName("Should get category spending for budget")
    void testGetCategorySpendingForBudget() {
        Budget savedBudget = budgetRepository.save(budget1);
        budgetCategory1.setBudget(savedBudget);
        budgetCategory2.setBudget(savedBudget);
        
        budgetCategoryRepository.save(budgetCategory1);
        budgetCategoryRepository.save(budgetCategory2);

        List<Object[]> categorySpending = budgetRepository.getCategorySpendingForBudget(savedBudget.getId());

        assertThat(categorySpending).hasSize(2);
        

        boolean foundGroceries = false;
        boolean foundDining = false;
        
        for (Object[] result : categorySpending) {
            Category category = (Category) result[0];
            BigDecimal spentAmount = (BigDecimal) result[1];
            
            if (category.getName().equals("Groceries")) {
                assertThat(spentAmount).isEqualByComparingTo(new BigDecimal("350.00"));
                foundGroceries = true;
            } else if (category.getName().equals("Dining")) {
                assertThat(spentAmount).isEqualByComparingTo(new BigDecimal("180.00"));
                foundDining = true;
            }
        }
        
        assertThat(foundGroceries).isTrue();
        assertThat(foundDining).isTrue();
    }

    @Test
    @DisplayName("Should update budget")
    void testUpdateBudget() {
        Budget saved = budgetRepository.save(budget1);
        
        saved.setName("Updated January Budget");
        saved.setStatus(BudgetStatus.COMPLETED);
        Budget updated = budgetRepository.save(saved);
        
        Optional<Budget> retrieved = budgetRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Updated January Budget");
        assertThat(retrieved.get().getStatus()).isEqualTo(BudgetStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should delete budget")
    void testDeleteBudget() {
        Budget saved = budgetRepository.save(budget1);
        
        budgetRepository.deleteById(saved.getId());
        
        Optional<Budget> retrieved = budgetRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all budgets")
    void testFindAllBudgets() {
        budgetRepository.save(budget1);
        budgetRepository.save(budget2);

        List<Budget> allBudgets = budgetRepository.findAll();

        assertThat(allBudgets).hasSize(2);
        assertThat(allBudgets).extracting("name")
                .containsExactlyInAnyOrder("January Budget", "February Budget");
    }

    @Test
    @DisplayName("Should handle budget with different statuses")
    void testBudgetWithDifferentStatuses() {
        Budget activeBudget = Budget.builder()
                .user(user1)
                .name("Active Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(BudgetStatus.ACTIVE)
                .build();

        Budget completedBudget = Budget.builder()
                .user(user1)
                .name("Completed Budget")
                .startDate(LocalDate.of(2023, 12, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .status(BudgetStatus.COMPLETED)
                .build();

        Budget upcomingBudget = Budget.builder()
                .user(user1)
                .name("Upcoming Budget")
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .status(BudgetStatus.UPCOMING)
                .build();

        budgetRepository.save(activeBudget);
        budgetRepository.save(completedBudget);
        budgetRepository.save(upcomingBudget);

        List<Budget> userBudgets = budgetRepository.findByUserId(user1.getId());

        assertThat(userBudgets).hasSize(3);
        assertThat(userBudgets).extracting("status")
                .containsExactlyInAnyOrder(BudgetStatus.ACTIVE, BudgetStatus.COMPLETED, BudgetStatus.UPCOMING);
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Budget> userBudgets = budgetRepository.findByUserId(user1.getId());
        List<Budget> allBudgets = budgetRepository.findAll();

        assertThat(userBudgets).isEmpty();
        assertThat(allBudgets).isEmpty();
    }

    @Test
    @DisplayName("Should handle budget without categories")
    void testBudgetWithoutCategories() {
        Budget budgetWithoutCategories = Budget.builder()
                .user(user1)
                .name("Empty Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(BudgetStatus.ACTIVE)
                .build();

        Budget saved = budgetRepository.save(budgetWithoutCategories);
        
        Optional<Budget> retrieved = budgetRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();

        assertThat(retrieved.get().getCategories()).satisfiesAnyOf(
            categories -> assertThat(categories).isNull(),
            categories -> assertThat(categories).isEmpty()
        );
    }

    @Test
    @DisplayName("Should handle multiple budgets for same user")
    void testMultipleBudgetsForSameUser() {
        Budget budget1 = Budget.builder()
                .user(user1)
                .name("Q1 Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .status(BudgetStatus.ACTIVE)
                .build();

        Budget budget2 = Budget.builder()
                .user(user1)
                .name("Q2 Budget")
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .status(BudgetStatus.UPCOMING)
                .build();

        budgetRepository.save(budget1);
        budgetRepository.save(budget2);

        List<Budget> userBudgets = budgetRepository.findByUserId(user1.getId());

        assertThat(userBudgets).hasSize(2);
        assertThat(userBudgets).extracting("name")
                .containsExactlyInAnyOrder("Q1 Budget", "Q2 Budget");
    }
} 