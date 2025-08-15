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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .createdAt(Instant.now())
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        anotherUser = User.builder()
                .email("another@example.com")
                .password("password123")
                .name("Another User")
                .createdAt(Instant.now())
                .build();
        anotherUser = entityManager.persistAndFlush(anotherUser);
    }

    @Test
    @DisplayName("Should find categories by user ID")
    void shouldFindCategoriesByUserId() {

        Category category1 = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(category1);

        Category category2 = Category.builder()
                .name("Transport")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(category2);

        Category category3 = Category.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .user(anotherUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(category3);


        List<Category> userCategories = categoryRepository.findByUserId(testUser.getId());


        assertThat(userCategories).hasSize(2);
        assertThat(userCategories).extracting("name").containsExactlyInAnyOrder("Food", "Transport");
    }

    @Test
    @DisplayName("Should find categories by user ID and type")
    void shouldFindCategoriesByUserIdAndType() {

        Category expenseCategory = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(expenseCategory);

        Category incomeCategory = Category.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(incomeCategory);


        List<Category> expenseCategories = categoryRepository.findByUserIdAndType(testUser.getId(), CategoryType.EXPENSE);
        List<Category> incomeCategories = categoryRepository.findByUserIdAndType(testUser.getId(), CategoryType.INCOME);


        assertThat(expenseCategories).hasSize(1);
        assertThat(expenseCategories.get(0).getName()).isEqualTo("Food");
        assertThat(incomeCategories).hasSize(1);
        assertThat(incomeCategories.get(0).getName()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("Should find category by ID and user ID")
    void shouldFindCategoryByIdAndUserId() {

        Category category = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        category = entityManager.persistAndFlush(category);


        Optional<Category> found = categoryRepository.findByIdAndUserId(category.getId(), testUser.getId());


        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Food");
    }

    @Test
    @DisplayName("Should not find category by ID and different user ID")
    void shouldNotFindCategoryByIdAndDifferentUserId() {

        Category category = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        category = entityManager.persistAndFlush(category);


        Optional<Category> found = categoryRepository.findByIdAndUserId(category.getId(), anotherUser.getId());


        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find category by name and user ID")
    void shouldFindCategoryByNameAndUserId() {

        Category category = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(category);


        Optional<Category> found = categoryRepository.findByNameAndUserId("Food", testUser.getId());


        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Food");
    }

    @Test
    @DisplayName("Should check if category exists by name and user ID")
    void shouldCheckIfCategoryExistsByNameAndUserId() {

        Category category = Category.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(category);

        boolean exists = categoryRepository.existsByNameAndUserId("Food", testUser.getId());
        boolean notExists = categoryRepository.existsByNameAndUserId("Food", anotherUser.getId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
} 