package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Category transferCategory;

    @BeforeEach
    void setup() {
        // Create a test user
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .createdAt(Instant.now())
                .build();
        testUser = userRepository.save(testUser);

        expenseCategory = Category.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .build();

        incomeCategory = Category.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .user(testUser)
                .build();

        transferCategory = Category.builder()
                .name("Bank Transfer")
                .type(CategoryType.TRANSFER)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve category")
    void testSaveAndRetrieveCategory() {
        Category saved = categoryRepository.save(expenseCategory);
        
        Optional<Category> retrieved = categoryRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Groceries");
        assertThat(retrieved.get().getType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    @DisplayName("Should find categories by type")
    void testFindByType() {
        categoryRepository.save(expenseCategory);
        categoryRepository.save(incomeCategory);
        categoryRepository.save(transferCategory);

        List<Category> expenseCategories = categoryRepository.findByType(CategoryType.EXPENSE);
        List<Category> incomeCategories = categoryRepository.findByType(CategoryType.INCOME);
        List<Category> transferCategories = categoryRepository.findByType(CategoryType.TRANSFER);

        assertThat(expenseCategories).hasSize(1);
        assertThat(expenseCategories.get(0).getName()).isEqualTo("Groceries");
        
        assertThat(incomeCategories).hasSize(1);
        assertThat(incomeCategories.get(0).getName()).isEqualTo("Salary");
        
        assertThat(transferCategories).hasSize(1);
        assertThat(transferCategories.get(0).getName()).isEqualTo("Bank Transfer");
    }

    @Test
    @DisplayName("Should update category")
    void testUpdateCategory() {
        Category saved = categoryRepository.save(expenseCategory);
        
        saved.setName("Updated Groceries");
        Category updated = categoryRepository.save(saved);
        
        Optional<Category> retrieved = categoryRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Updated Groceries");
    }

    @Test
    @DisplayName("Should delete category")
    void testDeleteCategory() {
        Category saved = categoryRepository.save(expenseCategory);
        
        categoryRepository.deleteById(saved.getId());
        
        Optional<Category> retrieved = categoryRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all categories")
    void testFindAllCategories() {
        categoryRepository.save(expenseCategory);
        categoryRepository.save(incomeCategory);
        categoryRepository.save(transferCategory);

        List<Category> allCategories = categoryRepository.findAll();

        assertThat(allCategories).hasSize(3);
        assertThat(allCategories).extracting("name")
                .containsExactlyInAnyOrder("Groceries", "Salary", "Bank Transfer");
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Category> expenseCategories = categoryRepository.findByType(CategoryType.EXPENSE);
        List<Category> allCategories = categoryRepository.findAll();

        assertThat(expenseCategories).isEmpty();
        assertThat(allCategories).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple categories of same type")
    void testFindMultipleCategoriesOfSameType() {
        Category groceries = Category.builder().name("Groceries").type(CategoryType.EXPENSE).user(testUser).build();
        Category dining = Category.builder().name("Dining").type(CategoryType.EXPENSE).user(testUser).build();
        Category transportation = Category.builder().name("Transportation").type(CategoryType.EXPENSE).user(testUser).build();

        categoryRepository.save(groceries);
        categoryRepository.save(dining);
        categoryRepository.save(transportation);

        List<Category> expenseCategories = categoryRepository.findByType(CategoryType.EXPENSE);

        assertThat(expenseCategories).hasSize(3);
        assertThat(expenseCategories).extracting("name")
                .containsExactlyInAnyOrder("Groceries", "Dining", "Transportation");
    }
} 