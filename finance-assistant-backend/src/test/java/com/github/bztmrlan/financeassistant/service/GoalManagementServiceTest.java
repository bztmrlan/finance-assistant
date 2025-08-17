package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.GoalRequest;
import com.github.bztmrlan.financeassistant.dto.GoalResponse;
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
class GoalManagementServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalManagementService goalManagementService;

    private UUID testUserId;
    private UUID testGoalId;
    private UUID testCategoryId;
    private User testUser;
    private Category testCategory;
    private Goal testGoal;
    private GoalRequest testGoalRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testGoalId = UUID.randomUUID();
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
        
        testGoal = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(testCategory)
                .name("Test Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .currency("USD")
                .completed(false)
                .build();
        
        testGoalRequest = new GoalRequest();
        testGoalRequest.setName("Test Goal");
        testGoalRequest.setTargetAmount(new BigDecimal("1000.00"));
        testGoalRequest.setTargetDate(LocalDate.now().plusMonths(6));
        testGoalRequest.setCurrency("USD");
        testGoalRequest.setCategoryId(testCategoryId);
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    void testCreateGoal_Success() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals("Test Goal", result.getName());
        assertEquals(new BigDecimal("1000.00"), result.getTargetAmount());
        assertEquals(BigDecimal.ZERO, result.getCurrentAmount());
        assertEquals("USD", result.getCurrency());
        assertFalse(result.isCompleted());
        
        verify(userRepository).findById(testUserId);
        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_WithoutCategory() {
        // Given
        testGoalRequest.setCategoryId(null);
        Goal goalWithoutCategory = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(null)
                .name("Test Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .currency("USD")
                .completed(false)
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(goalRepository.save(any(Goal.class))).thenReturn(goalWithoutCategory);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals("Test Goal", result.getName());
        assertNull(result.getCategoryName());
        
        verify(userRepository).findById(testUserId);
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_DefaultCurrency() {
        // Given
        testGoalRequest.setCurrency(null);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testGetUserGoals_Success() {
        // Given
        List<Goal> goals = List.of(testGoal);
        when(goalRepository.findByUserId(testUserId)).thenReturn(goals);

        // When
        List<GoalResponse> result = goalManagementService.getUserGoals(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Goal", result.get(0).getName());
        
        verify(goalRepository).findByUserId(testUserId);
    }

    @Test
    void testGetGoalById_Success() {
        // Given
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));

        // When
        Optional<GoalResponse> result = goalManagementService.getGoalById(testGoalId, testUserId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Goal", result.get().getName());
        assertEquals(testGoalId, result.get().getId());
        
        verify(goalRepository).findById(testGoalId);
    }

    @Test
    void testUpdateGoalProgress_Success() {
        // Given
        BigDecimal progressAmount = new BigDecimal("100.00");
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, progressAmount);

        // Then
        assertNotNull(result);
        assertEquals(progressAmount, result.getCurrentAmount());
        
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testUpdateGoalProgress_GoalCompleted() {
        // Given
        BigDecimal progressAmount = new BigDecimal("1000.00");
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, progressAmount);

        // Then
        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertEquals(new BigDecimal("1000.00"), result.getCurrentAmount());
        
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testDeleteGoal_Success() {
        // Given
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        doNothing().when(goalRepository).delete(testGoal);

        // When
        assertDoesNotThrow(() -> goalManagementService.deleteGoal(testGoalId, testUserId));

        // Then
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository).delete(testGoal);
    }

    // ==================== UNHAPPY PATH TESTS ====================

    @Test
    void testCreateGoal_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.createGoal(testUserId, testGoalRequest);
        });

        assertEquals("User not found", exception.getMessage());
        
        verify(userRepository).findById(testUserId);
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_CategoryNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.createGoal(testUserId, testGoalRequest);
        });

        assertEquals("Category not found or does not belong to user", exception.getMessage());
        
        verify(userRepository).findById(testUserId);
        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void testGetGoalById_NotFound() {
        // Given
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.empty());

        // When
        Optional<GoalResponse> result = goalManagementService.getGoalById(testGoalId, testUserId);

        // Then
        assertFalse(result.isPresent());
        
        verify(goalRepository).findById(testGoalId);
    }

    @Test
    void testGetGoalById_UserMismatch() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).build();
        Goal otherUserGoal = Goal.builder()
                .id(testGoalId)
                .user(otherUser)
                .name("Other User Goal")
                .build();
        
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(otherUserGoal));

        // When
        Optional<GoalResponse> result = goalManagementService.getGoalById(testGoalId, testUserId);

        // Then
        assertFalse(result.isPresent());
        
        verify(goalRepository).findById(testGoalId);
    }

    @Test
    void testUpdateGoalProgress_GoalNotFound() {
        // Given
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.updateGoalProgress(testGoalId, testUserId, new BigDecimal("100.00"));
        });

        assertEquals("Goal not found", exception.getMessage());
        
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void testUpdateGoalProgress_UserMismatch() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).build();
        Goal otherUserGoal = Goal.builder()
                .id(testGoalId)
                .user(otherUser)
                .name("Other User Goal")
                .build();
        
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(otherUserGoal));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.updateGoalProgress(testGoalId, testUserId, new BigDecimal("100.00"));
        });

        assertEquals("Goal not found", exception.getMessage());
        
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void testDeleteGoal_GoalNotFound() {
        // Given
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.deleteGoal(testGoalId, testUserId);
        });

        assertEquals("Goal not found", exception.getMessage());
        
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository, never()).delete(any(Goal.class));
    }

    @Test
    void testDeleteGoal_UserMismatch() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).build();
        Goal otherUserGoal = Goal.builder()
                .id(testGoalId)
                .user(otherUser)
                .name("Other User Goal")
                .build();
        
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(otherUserGoal));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goalManagementService.deleteGoal(testGoalId, testUserId);
        });

        assertEquals("Goal not found", exception.getMessage());
        
        verify(goalRepository).findById(testGoalId);
        verify(goalRepository, never()).delete(any(Goal.class));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testGetUserGoals_EmptyList() {
        // Given
        when(goalRepository.findByUserId(testUserId)).thenReturn(List.of());

        // When
        List<GoalResponse> result = goalManagementService.getUserGoals(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(goalRepository).findByUserId(testUserId);
    }

    @Test
    void testGetUserGoals_ProgressUpdateFailure() {
        // Given
        List<Goal> goals = List.of(testGoal);
        when(goalRepository.findByUserId(testUserId)).thenReturn(goals);

        // When
        List<GoalResponse> result = goalManagementService.getUserGoals(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Goal", result.get(0).getName());
        
        verify(goalRepository).findByUserId(testUserId);
    }

    @Test
    void testUpdateGoalProgress_NegativeAmount() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-50.00");
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, negativeAmount);

        // Then
        assertNotNull(result);
        assertEquals(negativeAmount, result.getCurrentAmount());
        
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testUpdateGoalProgress_ZeroAmount() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, zeroAmount);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getCurrentAmount());
        
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testUpdateGoalProgress_ExactTargetAmount() {
        // Given
        BigDecimal exactAmount = new BigDecimal("1000.00");
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, exactAmount);

        // Then
        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertEquals(exactAmount, result.getCurrentAmount());
        
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testUpdateGoalProgress_OverTargetAmount() {
        // Given
        BigDecimal overAmount = new BigDecimal("1200.00");
        when(goalRepository.findById(testGoalId)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // When
        GoalResponse result = goalManagementService.updateGoalProgress(testGoalId, testUserId, overAmount);

        // Then
        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertEquals(overAmount, result.getCurrentAmount());
        
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testCreateGoal_WithSpecialCharacters() {
        // Given
        testGoalRequest.setName("Goal with special chars: !@#$%^&*()");
        Goal specialCharGoal = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(testCategory)
                .name("Goal with special chars: !@#$%^&*()")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .currency("USD")
                .completed(false)
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(specialCharGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals("Goal with special chars: !@#$%^&*()", result.getName());
        
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_WithLongName() {
        // Given
        String longName = "A".repeat(255); // Very long name
        testGoalRequest.setName(longName);
        Goal longNameGoal = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(testCategory)
                .name(longName)
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .currency("USD")
                .completed(false)
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(longNameGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals(longName, result.getName());
        
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_WithPastTargetDate() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);
        testGoalRequest.setTargetDate(pastDate);
        Goal pastDateGoal = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(testCategory)
                .name("Test Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(pastDate)
                .currency("USD")
                .completed(false)
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(pastDateGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals(pastDate, result.getTargetDate());
        
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void testCreateGoal_WithVeryLargeAmount() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999999.99");
        testGoalRequest.setTargetAmount(largeAmount);
        Goal largeAmountGoal = Goal.builder()
                .id(testGoalId)
                .user(testUser)
                .category(testCategory)
                .name("Test Goal")
                .targetAmount(largeAmount)
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .currency("USD")
                .completed(false)
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId)).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(largeAmountGoal);

        // When
        GoalResponse result = goalManagementService.createGoal(testUserId, testGoalRequest);

        // Then
        assertNotNull(result);
        assertEquals(largeAmount, result.getTargetAmount());
        
        verify(goalRepository).save(any(Goal.class));
    }
} 