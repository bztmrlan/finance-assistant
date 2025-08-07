package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
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
public class BudgetCategoryRepositoryTest {

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category groceriesCategory;
    private Category diningCategory;
    private Category transportationCategory;
    private Budget budget1;
    private Budget budget2;
    private BudgetCategory budgetCategory1;
    private BudgetCategory budgetCategory2;
    private BudgetCategory budgetCategory3;

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
                .type(CategoryType.EXPENSE)
                .build();
        groceriesCategory = categoryRepository.save(groceriesCategory);

        diningCategory = Category.builder()
                .name("Dining")
                .type(CategoryType.EXPENSE)
                .build();
        diningCategory = categoryRepository.save(diningCategory);

        transportationCategory = Category.builder()
                .name("Transportation")
                .type(CategoryType.EXPENSE)
                .build();
        transportationCategory = categoryRepository.save(transportationCategory);


        budget1 = Budget.builder()
                .user(user1)
                .name("January Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(BudgetStatus.ACTIVE)
                .build();
        budget1 = budgetRepository.save(budget1);

        budget2 = Budget.builder()
                .user(user2)
                .name("February Budget")
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 2, 29))
                .status(BudgetStatus.UPCOMING)
                .build();
        budget2 = budgetRepository.save(budget2);


        budgetCategory1 = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("550.00"))
                .build();

        budgetCategory2 = BudgetCategory.builder()
                .budget(budget1)
                .category(diningCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(new BigDecimal("180.00"))
                .build();

        budgetCategory3 = BudgetCategory.builder()
                .budget(budget2)
                .category(transportationCategory)
                .limitAmount(new BigDecimal("300.00"))
                .spentAmount(new BigDecimal("320.00"))
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve budget category")
    void testSaveAndRetrieveBudgetCategory() {
        BudgetCategory saved = budgetCategoryRepository.save(budgetCategory1);
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLimitAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(retrieved.get().getSpentAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(retrieved.get().getBudget().getId()).isEqualTo(budget1.getId());
        assertThat(retrieved.get().getCategory().getId()).isEqualTo(groceriesCategory.getId());
    }

    @Test
    @DisplayName("Should find exceeded categories for budget")
    void testFindExceededCategories() {
        budgetCategoryRepository.save(budgetCategory1); // Exceeded (550 > 500)
        budgetCategoryRepository.save(budgetCategory2); // Not exceeded (180 < 200)
        budgetCategoryRepository.save(budgetCategory3); // Exceeded (320 > 300)

        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budget1.getId());

        assertThat(exceededCategories).hasSize(1);
        assertThat(exceededCategories.get(0).getCategory().getName()).isEqualTo("Groceries");
        assertThat(exceededCategories.get(0).getSpentAmount()).isEqualTo(new BigDecimal("550.00"));
    }

    @Test
    @DisplayName("Should check if budget category exists")
    void testExistsByBudgetAndCategory() {
        budgetCategoryRepository.save(budgetCategory1);

        boolean exists = budgetCategoryRepository.existsByBudgetAndCategory(budget1, groceriesCategory);
        boolean notExists = budgetCategoryRepository.existsByBudgetAndCategory(budget1, diningCategory);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should update budget category")
    void testUpdateBudgetCategory() {
        BudgetCategory saved = budgetCategoryRepository.save(budgetCategory1);
        
        saved.setSpentAmount(new BigDecimal("600.00"));
        saved.setLimitAmount(new BigDecimal("550.00"));
        BudgetCategory updated = budgetCategoryRepository.save(saved);
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSpentAmount()).isEqualTo(new BigDecimal("600.00"));
        assertThat(retrieved.get().getLimitAmount()).isEqualTo(new BigDecimal("550.00"));
    }

    @Test
    @DisplayName("Should delete budget category")
    void testDeleteBudgetCategory() {
        BudgetCategory saved = budgetCategoryRepository.save(budgetCategory1);
        
        budgetCategoryRepository.deleteById(saved.getId());
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all budget categories")
    void testFindAllBudgetCategories() {
        budgetCategoryRepository.save(budgetCategory1);
        budgetCategoryRepository.save(budgetCategory2);
        budgetCategoryRepository.save(budgetCategory3);

        List<BudgetCategory> allBudgetCategories = budgetCategoryRepository.findAll();

        assertThat(allBudgetCategories).hasSize(3);
        assertThat(allBudgetCategories).extracting("category.name")
                .containsExactlyInAnyOrder("Groceries", "Dining", "Transportation");
    }

    @Test
    @DisplayName("Should handle budget categories with different spending levels")
    void testBudgetCategoriesWithDifferentSpendingLevels() {
        BudgetCategory underBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("400.00"))
                .build();

        BudgetCategory atBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(diningCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(new BigDecimal("200.00"))
                .build();

        BudgetCategory overBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(transportationCategory)
                .limitAmount(new BigDecimal("300.00"))
                .spentAmount(new BigDecimal("350.00"))
                .build();

        budgetCategoryRepository.save(underBudget);
        budgetCategoryRepository.save(atBudget);
        budgetCategoryRepository.save(overBudget);

        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budget1.getId());

        assertThat(exceededCategories).hasSize(1);
        assertThat(exceededCategories.get(0).getCategory().getName()).isEqualTo("Transportation");
    }

    @Test
    @DisplayName("Should handle multiple exceeded categories for same budget")
    void testMultipleExceededCategoriesForSameBudget() {
        BudgetCategory exceeded1 = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("600.00"))
                .build();

        BudgetCategory exceeded2 = BudgetCategory.builder()
                .budget(budget1)
                .category(diningCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(new BigDecimal("250.00"))
                .build();

        budgetCategoryRepository.save(exceeded1);
        budgetCategoryRepository.save(exceeded2);

        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budget1.getId());

        assertThat(exceededCategories).hasSize(2);
        assertThat(exceededCategories).extracting("category.name")
                .containsExactlyInAnyOrder("Groceries", "Dining");
    }

    @Test
    @DisplayName("Should handle budget categories with zero amounts")
    void testBudgetCategoriesWithZeroAmounts() {
        BudgetCategory zeroBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(BigDecimal.ZERO)
                .spentAmount(BigDecimal.ZERO)
                .build();

        BudgetCategory saved = budgetCategoryRepository.save(zeroBudget);
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLimitAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(retrieved.get().getSpentAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle budget categories with large amounts")
    void testBudgetCategoriesWithLargeAmounts() {
        BudgetCategory largeBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("1000000.00"))
                .spentAmount(new BigDecimal("999999.99"))
                .build();

        BudgetCategory saved = budgetCategoryRepository.save(largeBudget);
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLimitAmount()).isEqualTo(new BigDecimal("1000000.00"));
        assertThat(retrieved.get().getSpentAmount()).isEqualTo(new BigDecimal("999999.99"));
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budget1.getId());
        List<BudgetCategory> allBudgetCategories = budgetCategoryRepository.findAll();

        assertThat(exceededCategories).isEmpty();
        assertThat(allBudgetCategories).isEmpty();
    }

    @Test
    @DisplayName("Should handle budget categories for different budgets")
    void testBudgetCategoriesForDifferentBudgets() {
        budgetCategoryRepository.save(budgetCategory1); // Budget 1, exceeded
        budgetCategoryRepository.save(budgetCategory3); // Budget 2, exceeded

        List<BudgetCategory> budget1Exceeded = budgetCategoryRepository.findExceededCategories(budget1.getId());
        List<BudgetCategory> budget2Exceeded = budgetCategoryRepository.findExceededCategories(budget2.getId());

        assertThat(budget1Exceeded).hasSize(1);
        assertThat(budget1Exceeded.get(0).getCategory().getName()).isEqualTo("Groceries");
        
        assertThat(budget2Exceeded).hasSize(1);
        assertThat(budget2Exceeded.get(0).getCategory().getName()).isEqualTo("Transportation");
    }

    @Test
    @DisplayName("Should handle budget categories with decimal amounts")
    void testBudgetCategoriesWithDecimalAmounts() {
        BudgetCategory decimalBudget = BudgetCategory.builder()
                .budget(budget1)
                .category(groceriesCategory)
                .limitAmount(new BigDecimal("500.50"))
                .spentAmount(new BigDecimal("500.75"))
                .build();

        BudgetCategory saved = budgetCategoryRepository.save(decimalBudget);
        
        Optional<BudgetCategory> retrieved = budgetCategoryRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLimitAmount()).isEqualTo(new BigDecimal("500.50"));
        assertThat(retrieved.get().getSpentAmount()).isEqualTo(new BigDecimal("500.75"));

        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budget1.getId());
        assertThat(exceededCategories).hasSize(1);
        assertThat(exceededCategories.get(0).getCategory().getName()).isEqualTo("Groceries");
    }
} 