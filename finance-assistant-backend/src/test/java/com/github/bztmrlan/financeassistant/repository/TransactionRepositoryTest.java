package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Transaction;
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
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category groceriesCategory;
    private Category salaryCategory;
    private Transaction transaction1;
    private Transaction transaction2;
    private Transaction transaction3;

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
                .user(user1)
                .build();
        groceriesCategory = categoryRepository.save(groceriesCategory);

        salaryCategory = Category.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .user(user1)
                .build();
        salaryCategory = categoryRepository.save(salaryCategory);


        transaction1 = Transaction.builder()
                .user(user1)
                .category(groceriesCategory)
                .date(LocalDate.of(2024, 1, 15))
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .description("Weekly groceries")
                .build();

        transaction2 = Transaction.builder()
                .user(user1)
                .category(salaryCategory)
                .date(LocalDate.of(2024, 1, 20))
                .amount(new BigDecimal("3000.00"))
                .currency("USD")
                .description("Monthly salary")
                .build();

        transaction3 = Transaction.builder()
                .user(user2)
                .category(groceriesCategory)
                .date(LocalDate.of(2024, 1, 18))
                .amount(new BigDecimal("75.00"))
                .currency("USD")
                .description("Organic groceries")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve transaction")
    void testSaveAndRetrieveTransaction() {
        Transaction saved = transactionRepository.save(transaction1);
        
        Optional<Transaction> retrieved = transactionRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(retrieved.get().getDescription()).isEqualTo("Weekly groceries");
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find transactions by user")
    void testFindByUserId() {
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);
        transactionRepository.save(transaction3);

        List<Transaction> user1Transactions = transactionRepository.findByUserId(user1.getId());
        List<Transaction> user2Transactions = transactionRepository.findByUserId(user2.getId());

        assertThat(user1Transactions).hasSize(2);
        assertThat(user2Transactions).hasSize(1);
        
        assertThat(user1Transactions).extracting("description")
                .containsExactlyInAnyOrder("Weekly groceries", "Monthly salary");
    }

    @Test
    @DisplayName("Should find transactions by user and date range")
    void testFindByUserIdAndDateBetween() {
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);
        transactionRepository.save(transaction3);

        LocalDate startDate = LocalDate.of(2024, 1, 16);
        LocalDate endDate = LocalDate.of(2024, 1, 25);

        List<Transaction> transactionsInRange = transactionRepository.findByUserIdAndDateBetween(
                user1.getId(), startDate, endDate);

        assertThat(transactionsInRange).hasSize(1);
        assertThat(transactionsInRange.get(0).getDescription()).isEqualTo("Monthly salary");
    }

    @Test
    @DisplayName("Should get spending by category")
    void testGetSpendingByCategory() {
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);
        transactionRepository.save(transaction3);

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        List<Object[]> spendingByCategory = transactionRepository.getSpendingByCategory(
                user1.getId(), startDate, endDate);

        assertThat(spendingByCategory).hasSize(2);
        

        boolean foundGroceries = false;
        boolean foundSalary = false;
        
        for (Object[] result : spendingByCategory) {
            Category category = (Category) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            
            if (category.getName().equals("Groceries")) {
                assertThat(amount).isEqualByComparingTo(new BigDecimal("50.00"));
                foundGroceries = true;
            } else if (category.getName().equals("Salary")) {
                assertThat(amount).isEqualByComparingTo(new BigDecimal("3000.00"));
                foundSalary = true;
            }
        }
        
        assertThat(foundGroceries).isTrue();
        assertThat(foundSalary).isTrue();
    }

    @Test
    @DisplayName("Should update transaction")
    void testUpdateTransaction() {
        Transaction saved = transactionRepository.save(transaction1);
        
        saved.setAmount(new BigDecimal("60.00"));
        saved.setDescription("Updated groceries");
        Transaction updated = transactionRepository.save(saved);
        
        Optional<Transaction> retrieved = transactionRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(retrieved.get().getDescription()).isEqualTo("Updated groceries");
    }

    @Test
    @DisplayName("Should delete transaction")
    void testDeleteTransaction() {
        Transaction saved = transactionRepository.save(transaction1);
        
        transactionRepository.deleteById(saved.getId());
        
        Optional<Transaction> retrieved = transactionRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty date range")
    void testEmptyDateRange() {
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 28);

        List<Transaction> transactionsInRange = transactionRepository.findByUserIdAndDateBetween(
                user1.getId(), startDate, endDate);

        assertThat(transactionsInRange).isEmpty();
    }

    @Test
    @DisplayName("Should handle transaction without category")
    void testTransactionWithoutCategory() {
        Transaction transactionWithoutCategory = Transaction.builder()
                .user(user1)
                .date(LocalDate.of(2024, 1, 15))
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .description("Cash withdrawal")
                .build();

        Transaction saved = transactionRepository.save(transactionWithoutCategory);
        
        Optional<Transaction> retrieved = transactionRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCategory()).isNull();
    }

    @Test
    @DisplayName("Should find all transactions")
    void testFindAllTransactions() {
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);
        transactionRepository.save(transaction3);

        List<Transaction> allTransactions = transactionRepository.findAll();

        assertThat(allTransactions).hasSize(3);
        assertThat(allTransactions).extracting("description")
                .containsExactlyInAnyOrder("Weekly groceries", "Monthly salary", "Organic groceries");
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Transaction> userTransactions = transactionRepository.findByUserId(user1.getId());
        List<Transaction> allTransactions = transactionRepository.findAll();

        assertThat(userTransactions).isEmpty();
        assertThat(allTransactions).isEmpty();
    }
} 