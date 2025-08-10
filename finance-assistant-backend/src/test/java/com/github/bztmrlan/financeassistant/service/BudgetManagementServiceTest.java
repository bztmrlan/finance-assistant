package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.*;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.enums.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetManagementServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetCategoryRepository budgetCategoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetManagementService budgetManagementService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private BudgetCategory testBudgetCategory;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("testuser")
                .email("test@example.com")
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Groceries")
                .type(com.github.bztmrlan.financeassistant.enums.CategoryType.EXPENSE)
                .build();

        testBudget = Budget.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Monthly Budget")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(BudgetStatus.ACTIVE)
                .categories(new ArrayList<>())
                .build();

        testBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("0.00"))
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.of(2024, 1, 15))
                .description("Grocery shopping")
                .build();

        testBudget.getCategories().add(testBudgetCategory);
    }

    @Test
    void testCreateBudget() {

        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(budgetCategoryRepository.saveAll(anyList())).thenReturn(Arrays.asList(testBudgetCategory));


        Budget result = budgetManagementService.createBudget(testBudget, Arrays.asList(testBudgetCategory));


        assertNotNull(result);
        assertEquals(BudgetStatus.ACTIVE, result.getStatus());
        verify(budgetRepository).save(testBudget);
        verify(budgetCategoryRepository).saveAll(Arrays.asList(testBudgetCategory));
    }

    @Test
    void testUpdateCategoryLimit() {

        BigDecimal newLimit = new BigDecimal("600.00");
        when(budgetCategoryRepository.findAll()).thenReturn(Arrays.asList(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);


        BudgetCategory result = budgetManagementService.updateCategoryLimit(
            testBudget.getId(), testCategory.getId(), newLimit);


        assertNotNull(result);
        assertEquals(newLimit, result.getLimitAmount());
        verify(budgetCategoryRepository).save(testBudgetCategory);
    }

    @Test
    void testAddCategoryLimit() {

        Category newCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Entertainment")
                .type(com.github.bztmrlan.financeassistant.enums.CategoryType.EXPENSE)
                .build();

        BudgetCategory newBudgetCategory = BudgetCategory.builder()
                .budget(testBudget)
                .category(newCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(BigDecimal.ZERO)
                .build();

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, newCategory)).thenReturn(false);
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(newBudgetCategory);


        BudgetCategory result = budgetManagementService.addCategoryLimit(
            testBudget.getId(), newCategory.getId(), new BigDecimal("200.00"));


        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getLimitAmount());
        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
    }

    @Test
    void testCalculateCategorySpending() {

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
            eq(testUser.getId()), eq(testCategory.getId()), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(testTransaction));


        BigDecimal result = budgetManagementService.calculateCategorySpending(
            testBudget.getId(), testCategory.getId());


        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void testUpdateBudgetSpending() {

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
            eq(testUser.getId()), eq(testCategory.getId()), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(testTransaction));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);


        budgetManagementService.updateBudgetSpending(testBudget.getId());


        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
        assertEquals(new BigDecimal("50.00"), testBudgetCategory.getSpentAmount());
    }

    @Test
    void testCheckBudgetLimitsAndCreateAlerts() {

        testBudgetCategory.setSpentAmount(new BigDecimal("600.00"));
        
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
            eq(testUser.getId()), eq(testCategory.getId()), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(testTransaction));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);
        when(budgetCategoryRepository.findExceededCategories(testBudget.getId()))
            .thenReturn(Arrays.asList(testBudgetCategory));
        when(alertRepository.findAll()).thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());


        budgetManagementService.checkBudgetLimitsAndCreateAlerts(testBudget.getId());


        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void testGetBudgetSummary() {

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
            eq(testUser.getId()), eq(testCategory.getId()), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(testTransaction));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);


        var summary = budgetManagementService.getBudgetSummary(testBudget.getId());


        assertNotNull(summary);
        assertEquals(testBudget.getId(), summary.getBudgetId());
        assertEquals("Monthly Budget", summary.getBudgetName());
        assertEquals(new BigDecimal("500.00"), summary.getTotalBudgeted());
        assertEquals(new BigDecimal("50.00"), summary.getTotalSpent());
        assertEquals(new BigDecimal("450.00"), summary.getRemainingAmount());
    }

    @Test
    void testGetUserBudgets() {

        when(budgetRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(testBudget));


        List<Budget> result = budgetManagementService.getUserBudgets(testUser.getId());


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBudget.getId(), result.get(0).getId());
    }

    @Test
    void testGetActiveUserBudgets() {

        Budget archivedBudget = Budget.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Completed Budget")
                .status(BudgetStatus.COMPLETED)
                .build();

        when(budgetRepository.findByUserId(testUser.getId()))
            .thenReturn(Arrays.asList(testBudget, archivedBudget));


        List<Budget> result = budgetManagementService.getActiveUserBudgets(testUser.getId());


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BudgetStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void testArchiveBudget() {

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);


        budgetManagementService.archiveBudget(testBudget.getId());


        assertEquals(BudgetStatus.COMPLETED, testBudget.getStatus());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void testCreateBudgetAlert() {

        testBudgetCategory.setSpentAmount(new BigDecimal("600.00"));
        
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(budgetCategoryRepository.findExceededCategories(testBudget.getId()))
            .thenReturn(Arrays.asList(testBudgetCategory));
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());


        budgetManagementService.checkBudgetLimitsAndCreateAlerts(testBudget.getId());


        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void testAddCategoryLimitWithExistingCategory() {

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, testCategory)).thenReturn(true);


        assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.addCategoryLimit(
                testBudget.getId(), testCategory.getId(), new BigDecimal("200.00"));
        });
    }

    @Test
    void testUpdateCategoryLimitNotFound() {

        when(budgetCategoryRepository.findAll()).thenReturn(new ArrayList<>());


        assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.updateCategoryLimit(
                testBudget.getId(), testCategory.getId(), new BigDecimal("600.00"));
        });
    }
} 