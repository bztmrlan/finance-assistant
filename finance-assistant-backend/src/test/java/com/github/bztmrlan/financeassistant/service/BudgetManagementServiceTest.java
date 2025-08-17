package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private UUID testUserId;
    private UUID testBudgetId;
    private UUID testCategoryId;
    private User testUser;
    private Budget testBudget;
    private Category testCategory;
    private BudgetCategory testBudgetCategory;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testBudgetId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .build();
        
        testCategory = Category.builder()
                .id(testCategoryId)
                .name("Test Category")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .build();
        
        testBudget = Budget.builder()
                .id(testBudgetId)
                .name("Test Budget")
                .user(testUser)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(BudgetStatus.ACTIVE)
                .build();
        
        testBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(BigDecimal.ZERO)
                .build();
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    void testCreateBudget_Success() {
        // Given
        List<BudgetCategory> categoryLimits = List.of(testBudgetCategory);
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(budgetCategoryRepository.saveAll(any())).thenReturn(categoryLimits);

        // When
        Budget result = budgetManagementService.createBudget(testBudget, categoryLimits);

        // Then
        assertNotNull(result);
        assertEquals(BudgetStatus.ACTIVE, result.getStatus());
        assertEquals(testBudgetId, result.getId());
        
        verify(budgetRepository).save(testBudget);
        verify(budgetCategoryRepository).saveAll(categoryLimits);
        
        // Verify that budget is set on each category limit
        categoryLimits.forEach(bc -> assertEquals(testBudget, bc.getBudget()));
    }

    @Test
    void testUpdateCategoryLimit_Success() {
        // Given
        BigDecimal newLimit = new BigDecimal("750.00");
        when(budgetCategoryRepository.findAll()).thenReturn(List.of(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.updateCategoryLimit(testBudgetId, testCategoryId, newLimit);

        // Then
        assertNotNull(result);
        assertEquals(newLimit, result.getLimitAmount());
        
        verify(budgetCategoryRepository).findAll();
        verify(budgetCategoryRepository).save(testBudgetCategory);
    }

    @Test
    void testAddCategoryLimit_Success() {
        // Given
        BigDecimal limitAmount = new BigDecimal("300.00");
        BudgetCategory newBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(limitAmount)
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, testCategory)).thenReturn(false);
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(newBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, limitAmount);

        // Then
        assertNotNull(result);
        assertEquals(limitAmount, result.getLimitAmount());
        assertEquals(BigDecimal.ZERO, result.getSpentAmount());
        assertEquals(testBudget, result.getBudget());
        assertEquals(testCategory, result.getCategory());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository).existsByBudgetAndCategory(testBudget, testCategory);
        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
    }

    @Test
    void testDeleteCategoryLimit_Success() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.findByBudgetAndCategory(testBudget, testCategory))
                .thenReturn(Optional.of(testBudgetCategory));
        doNothing().when(budgetCategoryRepository).deleteById(testBudgetCategory.getId());

        // When
        assertDoesNotThrow(() -> budgetManagementService.deleteCategoryLimit(testBudgetId, testCategoryId));

        // Then
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository).findByBudgetAndCategory(testBudget, testCategory);
        verify(budgetCategoryRepository).deleteById(testBudgetCategory.getId());
    }

    @Test
    void testGetUserBudgets_Success() {
        // Given
        List<Budget> budgets = List.of(testBudget);
        when(budgetRepository.findByUserIdWithUserCategories(testUserId)).thenReturn(budgets);

        // When
        List<Budget> result = budgetManagementService.getUserBudgets(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBudget, result.get(0));
        
        verify(budgetRepository).findByUserIdWithUserCategories(testUserId);
    }

    @Test
    void testGetActiveUserBudgets_Success() {
        // Given
        List<Budget> budgets = List.of(testBudget);
        when(budgetRepository.findByUserIdWithUserCategories(testUserId)).thenReturn(budgets);

        // When
        List<Budget> result = budgetManagementService.getActiveUserBudgets(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BudgetStatus.ACTIVE, result.get(0).getStatus());
        
        verify(budgetRepository).findByUserIdWithUserCategories(testUserId);
    }

    @Test
    void testArchiveBudget_Success() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // When
        budgetManagementService.archiveBudget(testBudgetId);

        // Then
        assertEquals(BudgetStatus.COMPLETED, testBudget.getStatus());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void testDeleteBudget_Success() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetCategoryRepository).deleteByBudget(testBudget);
        doNothing().when(budgetRepository).deleteById(testBudgetId);

        // When
        budgetManagementService.deleteBudget(testBudgetId);

        // Then
        verify(budgetCategoryRepository).deleteByBudget(testBudget);
        verify(budgetRepository).deleteById(testBudgetId);
    }

    // ==================== UNHAPPY PATH TESTS ====================

    @Test
    void testUpdateCategoryLimit_NotFound() {
        // Given
        when(budgetCategoryRepository.findAll()).thenReturn(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.updateCategoryLimit(testBudgetId, testCategoryId, new BigDecimal("100.00"));
        });

        assertEquals("Budget category not found", exception.getMessage());
        
        verify(budgetCategoryRepository).findAll();
        verify(budgetCategoryRepository, never()).save(any(BudgetCategory.class));
    }

    @Test
    void testAddCategoryLimit_BudgetNotFound() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, new BigDecimal("100.00"));
        });

        assertEquals("Budget not found", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(budgetCategoryRepository, never()).save(any(BudgetCategory.class));
    }

    @Test
    void testAddCategoryLimit_CategoryNotFound() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, new BigDecimal("100.00"));
        });

        assertEquals("Category not found", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository, never()).save(any(BudgetCategory.class));
    }

    @Test
    void testAddCategoryLimit_AlreadyExists() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, testCategory)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, new BigDecimal("100.00"));
        });

        assertEquals("Category limit already exists for this budget", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository).existsByBudgetAndCategory(testBudget, testCategory);
        verify(budgetCategoryRepository, never()).save(any(BudgetCategory.class));
    }

    @Test
    void testDeleteCategoryLimit_BudgetNotFound() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.deleteCategoryLimit(testBudgetId, testCategoryId);
        });

        assertEquals("Budget not found", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(budgetCategoryRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testDeleteCategoryLimit_CategoryNotFound() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            budgetManagementService.deleteCategoryLimit(testBudgetId, testCategoryId);
        });

        assertEquals("Category not found", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testDeleteCategoryLimit_BudgetCategoryNotFound() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.findByBudgetAndCategory(testBudget, testCategory))
                .thenReturn(Optional.empty());

        // When
        assertDoesNotThrow(() -> budgetManagementService.deleteCategoryLimit(testBudgetId, testCategoryId));

        // Then
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository).findByBudgetAndCategory(testBudget, testCategory);
        verify(budgetCategoryRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testGetUserBudgets_EmptyList() {
        // Given
        when(budgetRepository.findByUserIdWithUserCategories(testUserId)).thenReturn(List.of());

        // When
        List<Budget> result = budgetManagementService.getUserBudgets(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(budgetRepository).findByUserIdWithUserCategories(testUserId);
    }

    @Test
    void testGetActiveUserBudgets_EmptyList() {
        // Given
        when(budgetRepository.findByUserIdWithUserCategories(testUserId)).thenReturn(List.of());

        // When
        List<Budget> result = budgetManagementService.getActiveUserBudgets(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(budgetRepository).findByUserIdWithUserCategories(testUserId);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testCreateBudget_EmptyCategoryLimits() {
        // Given
        List<BudgetCategory> categoryLimits = List.of();
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(budgetCategoryRepository.saveAll(any())).thenReturn(categoryLimits);

        // When
        Budget result = budgetManagementService.createBudget(testBudget, categoryLimits);

        // Then
        assertNotNull(result);
        assertEquals(BudgetStatus.ACTIVE, result.getStatus());
        
        verify(budgetRepository).save(testBudget);
        verify(budgetCategoryRepository).saveAll(categoryLimits);
    }

    @Test
    void testUpdateCategoryLimit_ZeroLimit() {
        // Given
        BigDecimal zeroLimit = BigDecimal.ZERO;
        when(budgetCategoryRepository.findAll()).thenReturn(List.of(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.updateCategoryLimit(testBudgetId, testCategoryId, zeroLimit);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getLimitAmount());
        
        verify(budgetCategoryRepository).findAll();
        verify(budgetCategoryRepository).save(testBudgetCategory);
    }

    @Test
    void testAddCategoryLimit_ZeroLimit() {
        // Given
        BigDecimal zeroLimit = BigDecimal.ZERO;
        BudgetCategory zeroLimitBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(zeroLimit)
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, testCategory)).thenReturn(false);
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(zeroLimitBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, zeroLimit);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getLimitAmount());
        
        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
    }

    @Test
    void testAddCategoryLimit_NegativeLimit() {
        // Given
        BigDecimal negativeLimit = new BigDecimal("-50.00");
        BudgetCategory negativeLimitBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(negativeLimit)
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.existsByBudgetAndCategory(testBudget, testCategory)).thenReturn(false);
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(negativeLimitBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.addCategoryLimit(testBudgetId, testCategoryId, negativeLimit);

        // Then
        assertNotNull(result);
        assertEquals(negativeLimit, result.getLimitAmount());
        
        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
    }

    @Test
    void testCreateBudget_MultipleCategoryLimits() {
        // Given
        BudgetCategory categoryLimit1 = BudgetCategory.builder()
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("300.00"))
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        BudgetCategory categoryLimit2 = BudgetCategory.builder()
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("200.00"))
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        List<BudgetCategory> categoryLimits = List.of(categoryLimit1, categoryLimit2);
        
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(budgetCategoryRepository.saveAll(any())).thenReturn(categoryLimits);

        // When
        Budget result = budgetManagementService.createBudget(testBudget, categoryLimits);

        // Then
        assertNotNull(result);
        assertEquals(BudgetStatus.ACTIVE, result.getStatus());
        
        verify(budgetRepository).save(testBudget);
        verify(budgetCategoryRepository).saveAll(categoryLimits);
        
        // Verify that budget is set on each category limit
        categoryLimits.forEach(bc -> assertEquals(testBudget, bc.getBudget()));
    }

    @Test
    void testUpdateCategoryLimit_NegativeLimit() {
        // Given
        BigDecimal negativeLimit = new BigDecimal("-100.00");
        when(budgetCategoryRepository.findAll()).thenReturn(List.of(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class))).thenReturn(testBudgetCategory);

        // When
        BudgetCategory result = budgetManagementService.updateCategoryLimit(testBudgetId, testCategoryId, negativeLimit);

        // Then
        assertNotNull(result);
        assertEquals(negativeLimit, result.getLimitAmount());
        
        verify(budgetCategoryRepository).findAll();
        verify(budgetCategoryRepository).save(testBudgetCategory);
    }

    @Test
    void testDeleteCategoryLimit_ExceptionHandling() {
        // Given
        when(budgetRepository.findById(testBudgetId)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(budgetCategoryRepository.findByBudgetAndCategory(testBudget, testCategory))
                .thenReturn(Optional.of(testBudgetCategory));
        doThrow(new RuntimeException("Database error")).when(budgetCategoryRepository).deleteById(testBudgetCategory.getId());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            budgetManagementService.deleteCategoryLimit(testBudgetId, testCategoryId);
        });

        assertEquals("Database error", exception.getMessage());
        
        verify(budgetRepository).findById(testBudgetId);
        verify(categoryRepository).findById(testCategoryId);
        verify(budgetCategoryRepository).findByBudgetAndCategory(testBudget, testCategory);
        verify(budgetCategoryRepository).deleteById(testBudgetCategory.getId());
    }
} 