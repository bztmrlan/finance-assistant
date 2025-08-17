package com.github.bztmrlan.financeassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import com.github.bztmrlan.financeassistant.service.BudgetEvaluationService;
import com.github.bztmrlan.financeassistant.service.BudgetManagementService;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class BudgetControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private BudgetManagementService budgetManagementService;

    @MockBean
    private BudgetEvaluationService budgetEvaluationService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Budget testBudget;
    private BudgetCategory testBudgetCategory;
    private Category testCategory;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .createdAt(Instant.now())
                .build();


        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Groceries")
                .type(com.github.bztmrlan.financeassistant.enums.CategoryType.EXPENSE)
                .build();

        testBudget = Budget.builder()
                .id(UUID.randomUUID())
                .name("Monthly Budget")
                .user(testUser)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(BudgetStatus.ACTIVE)
                .categories(new ArrayList<>())
                .build();


        testBudgetCategory = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("100.00"))
                .build();


        CustomUserDetailsService.CustomUserDetails userDetails = 
            new CustomUserDetailsService.CustomUserDetails(
                testUser.getId(), 
                testUser.getEmail(), 
                testUser.getPassword(), 
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))
            );
        mockAuthentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testCreateBudget_Success() throws Exception {

        BudgetController.CreateBudgetRequest request = new BudgetController.CreateBudgetRequest();
        request.setBudget(testBudget);
        

        BudgetController.CategoryLimitRequest categoryLimit = new BudgetController.CategoryLimitRequest();
        categoryLimit.setCategoryId(testCategory.getId());
        categoryLimit.setLimitAmount(new BigDecimal("500.00"));
        request.setCategoryLimits(Arrays.asList(categoryLimit));

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetManagementService.createBudget(any(Budget.class), anyList()))
                .thenReturn(testBudget);


        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBudget.getId().toString()))
                .andExpect(jsonPath("$.name").value("Monthly Budget"));

        verify(budgetManagementService).createBudget(any(Budget.class), anyList());
    }

    @Test
    void testCreateBudget_UserNotFound() throws Exception {

        BudgetController.CreateBudgetRequest request = new BudgetController.CreateBudgetRequest();
        request.setBudget(testBudget);

        BudgetController.CategoryLimitRequest categoryLimit = new BudgetController.CategoryLimitRequest();
        categoryLimit.setCategoryId(testCategory.getId());
        categoryLimit.setLimitAmount(new BigDecimal("500.00"));
        request.setCategoryLimits(Arrays.asList(categoryLimit));

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isBadRequest());

        verify(budgetManagementService, never()).createBudget(any(), anyList());
    }

    @Test
    void testCreateBudget_ServiceException() throws Exception {

        BudgetController.CreateBudgetRequest request = new BudgetController.CreateBudgetRequest();
        request.setBudget(testBudget);

        BudgetController.CategoryLimitRequest categoryLimit = new BudgetController.CategoryLimitRequest();
        categoryLimit.setCategoryId(testCategory.getId());
        categoryLimit.setLimitAmount(new BigDecimal("500.00"));
        request.setCategoryLimits(Arrays.asList(categoryLimit));

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetManagementService.createBudget(any(Budget.class), anyList()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserBudgets_Success() throws Exception {

        List<Budget> budgets = Arrays.asList(testBudget);
        when(budgetManagementService.getUserBudgets(testUser.getId())).thenReturn(budgets);
        when(budgetManagementService.updateBudgetSpending(testBudget.getId())).thenReturn(testBudget);

        mockMvc.perform(get("/api/budgets")
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testBudget.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Monthly Budget"));

        verify(budgetManagementService).getUserBudgets(testUser.getId());
        verify(budgetManagementService).updateBudgetSpending(testBudget.getId());
    }

    @Test
    void testGetUserBudgets_ServiceException() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/budgets")
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetActiveUserBudgets_Success() throws Exception {

        List<Budget> activeBudgets = Arrays.asList(testBudget);
        when(budgetManagementService.getActiveUserBudgets(testUser.getId())).thenReturn(activeBudgets);
        when(budgetManagementService.updateBudgetSpending(testBudget.getId())).thenReturn(testBudget);

        mockMvc.perform(get("/api/budgets/active")
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testBudget.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(budgetManagementService).getActiveUserBudgets(testUser.getId());
        verify(budgetManagementService).updateBudgetSpending(testBudget.getId());
    }

    @Test
    void testGetActiveUserBudgets_ServiceException() throws Exception {

        when(budgetManagementService.getActiveUserBudgets(testUser.getId()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/budgets/active")
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetBudgetSummary_Success() throws Exception {
        BudgetManagementService.BudgetSummary summary = BudgetManagementService.BudgetSummary.builder()
                .budgetId(testBudget.getId())
                .budgetName("Monthly Budget")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(BudgetStatus.ACTIVE)
                .totalBudgeted(new BigDecimal("1000.00"))
                .totalSpent(new BigDecimal("300.00"))
                .remainingAmount(new BigDecimal("700.00"))
                .categorySummaries(Arrays.asList(
                    BudgetManagementService.CategorySummary.builder()
                        .categoryId(testCategory.getId())
                        .categoryName(testCategory.getName())
                        .limitAmount(testBudgetCategory.getLimitAmount())
                        .spentAmount(testBudgetCategory.getSpentAmount())
                        .progressPercentage(new BigDecimal("30.00"))
                        .build()
                ))
                .build();

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.getBudgetSummary(testBudget.getId())).thenReturn(summary);

        mockMvc.perform(get("/api/budgets/{budgetId}/summary", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(testBudget.getId().toString()))
                .andExpect(jsonPath("$.budgetName").value("Monthly Budget"))
                .andExpect(jsonPath("$.totalBudgeted").value(1000.0))
                .andExpect(jsonPath("$.totalSpent").value(300.0));

        verify(budgetManagementService).getBudgetSummary(testBudget.getId());
    }

    @Test
    void testGetBudgetSummary_BudgetNotFound() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/budgets/{budgetId}/summary", UUID.randomUUID())
                .principal(mockAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetBudgetSummary_ServiceException() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.getBudgetSummary(testBudget.getId()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/budgets/{budgetId}/summary", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateCategoryLimit_Success() throws Exception {

        BudgetController.UpdateLimitRequest request = new BudgetController.UpdateLimitRequest();
        request.setNewLimit(new BigDecimal("600.00"));

        BudgetCategory updatedCategory = BudgetCategory.builder()
                .id(testBudgetCategory.getId())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("600.00"))
                .spentAmount(new BigDecimal("100.00"))
                .build();

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.updateCategoryLimit(testBudget.getId(), testCategory.getId(), new BigDecimal("600.00")))
                .thenReturn(updatedCategory);

        mockMvc.perform(put("/api/budgets/{budgetId}/categories/{categoryId}/limit", 
                testBudget.getId(), testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(600.0));

        verify(budgetManagementService).updateCategoryLimit(testBudget.getId(), testCategory.getId(), new BigDecimal("600.00"));
    }

    @Test
    void testUpdateCategoryLimit_BudgetNotFound() throws Exception {

        BudgetController.UpdateLimitRequest request = new BudgetController.UpdateLimitRequest();
        request.setNewLimit(new BigDecimal("600.00"));

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(put("/api/budgets/{budgetId}/categories/{categoryId}/limit", 
                UUID.randomUUID(), testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCategoryLimit_ServiceException() throws Exception {
        BudgetController.UpdateLimitRequest request = new BudgetController.UpdateLimitRequest();
        request.setNewLimit(new BigDecimal("600.00"));

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.updateCategoryLimit(any(), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(put("/api/budgets/{budgetId}/categories/{categoryId}/limit", 
                testBudget.getId(), testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddCategoryLimit_Success() throws Exception {

        BudgetController.AddCategoryLimitRequest request = new BudgetController.AddCategoryLimitRequest();
        request.setCategoryId(testCategory.getId());
        request.setLimitAmount(new BigDecimal("300.00"));

        BudgetCategory newCategoryLimit = BudgetCategory.builder()
                .id(UUID.randomUUID())
                .budget(testBudget)
                .category(testCategory)
                .limitAmount(new BigDecimal("300.00"))
                .spentAmount(BigDecimal.ZERO)
                .build();

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.addCategoryLimit(testBudget.getId(), testCategory.getId(), new BigDecimal("300.00")))
                .thenReturn(newCategoryLimit);

        mockMvc.perform(post("/api/budgets/{budgetId}/categories", testBudget.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(300.0));

        verify(budgetManagementService).addCategoryLimit(testBudget.getId(), testCategory.getId(), new BigDecimal("300.00"));
    }

    @Test
    void testAddCategoryLimit_BudgetNotFound() throws Exception {

        BudgetController.AddCategoryLimitRequest request = new BudgetController.AddCategoryLimitRequest();
        request.setCategoryId(testCategory.getId());
        request.setLimitAmount(new BigDecimal("300.00"));

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(post("/api/budgets/{budgetId}/categories", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddCategoryLimit_ServiceException() throws Exception {

        BudgetController.AddCategoryLimitRequest request = new BudgetController.AddCategoryLimitRequest();
        request.setCategoryId(testCategory.getId());
        request.setLimitAmount(new BigDecimal("300.00"));

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetManagementService.addCategoryLimit(any(), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/budgets/{budgetId}/categories", testBudget.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(mockAuthentication))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEvaluateBudget_Success() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        doNothing().when(budgetManagementService).checkBudgetLimitsAndCreateAlerts(testBudget.getId());

        mockMvc.perform(post("/api/budgets/{budgetId}/evaluate", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().string("Budget evaluation completed successfully"));

        verify(budgetManagementService).checkBudgetLimitsAndCreateAlerts(testBudget.getId());
    }

    @Test
    void testEvaluateBudget_BudgetNotFound() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(post("/api/budgets/{budgetId}/evaluate", UUID.randomUUID())
                .principal(mockAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void testEvaluateBudget_ServiceException() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        doThrow(new RuntimeException("Service error"))
                .when(budgetManagementService).checkBudgetLimitsAndCreateAlerts(testBudget.getId());

        mockMvc.perform(post("/api/budgets/{budgetId}/evaluate", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetBudgetsNeedingAttention_Success() throws Exception {

        List<Budget> attentionBudgets = Arrays.asList(testBudget);
        when(budgetManagementService.getActiveUserBudgets(testUser.getId())).thenReturn(attentionBudgets);

        mockMvc.perform(get("/api/budgets/attention-needed")
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testBudget.getId().toString()));

        verify(budgetManagementService).getActiveUserBudgets(testUser.getId());
    }

    @Test
    void testGetBudgetsNeedingAttention_ServiceException() throws Exception {

        when(budgetManagementService.getActiveUserBudgets(testUser.getId()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/budgets/attention-needed")
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testArchiveBudget_Success() throws Exception {

        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        doNothing().when(budgetManagementService).archiveBudget(testBudget.getId());

        mockMvc.perform(put("/api/budgets/{budgetId}/archive", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().string("Budget archived successfully"));

        verify(budgetManagementService).archiveBudget(testBudget.getId());
    }

    @Test
    void testArchiveBudget_BudgetNotFound() throws Exception {
        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(put("/api/budgets/{budgetId}/archive", UUID.randomUUID())
                .principal(mockAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void testArchiveBudget_ServiceException() throws Exception {
        when(budgetManagementService.getUserBudgets(testUser.getId()))
                .thenReturn(Arrays.asList(testBudget));
        doThrow(new RuntimeException("Service error"))
                .when(budgetManagementService).archiveBudget(testBudget.getId());

        mockMvc.perform(put("/api/budgets/{budgetId}/archive", testBudget.getId())
                .principal(mockAuthentication))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testAuthenticationNotAvailable() throws Exception {

        Authentication invalidAuth = new UsernamePasswordAuthenticationToken("invalid", null, null);

        mockMvc.perform(get("/api/budgets")
                .principal(invalidAuth))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testNullAuthentication() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isInternalServerError());
    }
} 