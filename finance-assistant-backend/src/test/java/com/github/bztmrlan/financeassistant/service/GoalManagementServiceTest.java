package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.GoalRequest;
import com.github.bztmrlan.financeassistant.dto.GoalResponse;
import com.github.bztmrlan.financeassistant.enums.SourceType;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Goal;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.AlertRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.GoalRepository;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
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

    @InjectMocks
    private GoalManagementService goalManagementService;

    private User testUser;
    private Category testCategory;
    private Goal testGoal;
    private GoalRequest testGoalRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Savings")
                .build();

        testGoal = Goal.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .name("Save $1000")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(3))
                .currency("USD")
                .completed(false)
                .build();

        testGoalRequest = GoalRequest.builder()
                .name("Save $1000")
                .targetAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.now().plusMonths(3))
                .categoryId(testCategory.getId())
                .currency("USD")
                .build();
    }

    @Test
    void createGoal_Success() {

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);


        GoalResponse result = goalManagementService.createGoal(testUser.getId(), testGoalRequest);


        assertNotNull(result);
        assertEquals(testGoal.getName(), result.getName());
        assertEquals(testGoal.getTargetAmount(), result.getTargetAmount());
        assertEquals(testGoal.getTargetDate(), result.getTargetDate());
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void createGoal_UserNotFound() {

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());


        assertThrows(RuntimeException.class, () -> 
            goalManagementService.createGoal(testUser.getId(), testGoalRequest));
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void getUserGoals_Success() {

        List<Goal> goals = List.of(testGoal);
        when(goalRepository.findByUserId(testUser.getId())).thenReturn(goals);


        List<GoalResponse> result = goalManagementService.getUserGoals(testUser.getId());


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testGoal.getName(), result.get(0).getName());
    }

    @Test
    void getGoalById_Success() {

        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.of(testGoal));


        Optional<GoalResponse> result = goalManagementService.getGoalById(testGoal.getId(), testUser.getId());


        assertTrue(result.isPresent());
        assertEquals(testGoal.getName(), result.get().getName());
    }

    @Test
    void getGoalById_NotFound() {

        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.empty());


        Optional<GoalResponse> result = goalManagementService.getGoalById(testGoal.getId(), testUser.getId());


        assertFalse(result.isPresent());
    }

    @Test
    void updateGoalProgress_Success() {

        BigDecimal progressAmount = new BigDecimal("100.00");
        

        Goal mockGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(testGoal.getTargetAmount())
                .currentAmount(testGoal.getCurrentAmount())
                .targetDate(testGoal.getTargetDate())
                .currency(testGoal.getCurrency())
                .completed(testGoal.isCompleted())
                .build();
        

        Goal savedGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(testGoal.getTargetAmount())
                .currentAmount(progressAmount)
                .targetDate(testGoal.getTargetDate())
                .currency(testGoal.getCurrency())
                .completed(false)
                .build();
        
        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.of(mockGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);


        GoalResponse result = goalManagementService.updateGoalProgress(testGoal.getId(), testUser.getId(), progressAmount);


        assertNotNull(result);
        assertEquals(progressAmount, result.getCurrentAmount());
        assertEquals(new BigDecimal("10.00"), result.getProgressPercentage());
        verify(goalRepository).save(any(Goal.class));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void updateGoalProgress_GoalCompleted() {

        BigDecimal progressAmount = new BigDecimal("100.00");
        

        Goal mockGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("900.00"))
                .targetDate(testGoal.getTargetDate())
                .currency(testGoal.getCurrency())
                .completed(false)
                .build();
        

        Goal completedGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("1000.00"))
                .targetDate(testGoal.getTargetDate())
                .currency(testGoal.getCurrency())
                .completed(true)
                .build();
        
        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.of(mockGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(completedGoal);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());


        GoalResponse result = goalManagementService.updateGoalProgress(testGoal.getId(), testUser.getId(), progressAmount);


        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertEquals(new BigDecimal("1000.00"), result.getCurrentAmount());
        assertEquals(new BigDecimal("100.00"), result.getProgressPercentage());
        verify(goalRepository).save(any(Goal.class));
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void deleteGoal_Success() {

        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.of(testGoal));


        goalManagementService.deleteGoal(testGoal.getId(), testUser.getId());


        verify(goalRepository).delete(testGoal);
    }

    @Test
    void deleteGoal_GoalNotFound() {

        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.empty());


        assertThrows(RuntimeException.class, () -> 
            goalManagementService.deleteGoal(testGoal.getId(), testUser.getId()));
        verify(goalRepository, never()).delete(any(Goal.class));
    }

    @Test
    void updateGoalProgress_GoalAtRisk() {

        BigDecimal progressAmount = new BigDecimal("50.00");
        

        Goal mockGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("200.00"))
                .targetDate(LocalDate.now().plusDays(15))
                .currency(testGoal.getCurrency())
                .completed(false)
                .build();
        
        Goal savedGoal = Goal.builder()
                .id(testGoal.getId())
                .user(testUser)
                .category(testCategory)
                .name(testGoal.getName())
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("200.00"))
                .targetDate(LocalDate.now().plusDays(15))
                .currency(testGoal.getCurrency())
                .completed(false)
                .build();
        
        when(goalRepository.findById(testGoal.getId())).thenReturn(Optional.of(mockGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());


        GoalResponse result = goalManagementService.updateGoalProgress(testGoal.getId(), testUser.getId(), progressAmount);


        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getCurrentAmount());
        assertEquals(new BigDecimal("20.00"), result.getProgressPercentage());
        verify(goalRepository).save(any(Goal.class));
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void evaluateGoalsForUser_Success() {

        List<Goal> goals = List.of(testGoal);
        when(goalRepository.findByUserId(testUser.getId())).thenReturn(goals);


        goalManagementService.evaluateGoalsForUser(testUser.getId());


        verify(goalRepository).findByUserId(testUser.getId());
    }
} 