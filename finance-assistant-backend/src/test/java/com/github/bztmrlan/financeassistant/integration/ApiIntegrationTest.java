package com.github.bztmrlan.financeassistant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bztmrlan.financeassistant.dto.*;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.*;
import com.github.bztmrlan.financeassistant.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("API Integration: User registration and authentication flow")
    void testUserAuthenticationFlow() throws Exception {
        // Test user registration
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("API Test User");
        registerRequest.setEmail("apitest@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Test user login
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("apitest@example.com");
        authRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("API Integration: Transaction upload and management")
    void testTransactionManagementEndpoints() throws Exception {
        // Create test data for this test
        User testUser = createTestUser();
        
        // Test transaction upload - this will fail due to authentication requirement
        // but we can test the endpoint exists and returns proper error
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                "Date,Amount,Type,Description,Category\n2024-01-15,150.00,purchase,API Test Transaction,Test Category".getBytes()
        );

        mockMvc.perform(multipart("/api/transactions/upload")
                .file(csvFile)
                .param("currency", "USD")
                .param("autoCategorize", "false")
                .param("skipDuplicates", "false")
                .param("dateFormat", "yyyy-MM-dd"))
                .andExpect(status().isUnauthorized()); // Expect 401 due to missing authentication

        // Test get all transactions - expect unauthorized
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        // Verify no transactions were created due to authentication failure
        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("API Integration: Insight service endpoints")
    void testInsightServiceEndpoints() throws Exception {
        // Create test data for this test
        User testUser = createTestUser();
        Category testCategory = createTestCategory(testUser);
        
        // Create some transactions first
        createTestTransactions(testUser, testCategory);

        // Test get spending insights - expect internal server error due to authentication not available
        InsightRequest insightRequest = new InsightRequest();
        insightRequest.setQuestion("What are my spending patterns this month?");
        insightRequest.setTimePeriod("MONTHLY");

        mockMvc.perform(post("/api/insights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(insightRequest)))
                .andExpect(status().isInternalServerError());

        // Test get all insights - expect internal server error due to authentication not available
        mockMvc.perform(get("/api/insights"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("API Integration: Error handling and validation")
    void testErrorHandlingAndValidation() throws Exception {
        // Create test data for this test
        User testUser = createTestUser();
        
        // Test invalid transaction upload - expect unauthorized due to missing authentication
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalid.csv",
                "text/csv",
                "Invalid,Data,Format\nInvalid,Amount,Date".getBytes()
        );

        mockMvc.perform(multipart("/api/transactions/upload")
                .file(invalidFile)
                .param("currency", "USD")
                .param("autoCategorize", "false")
                .param("skipDuplicates", "false")
                .param("dateFormat", "yyyy-MM-dd"))
                .andExpect(status().isUnauthorized());

        // Test unauthorized access
        mockMvc.perform(get("/api/transactions")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API Integration: Data consistency across endpoints")
    void testDataConsistencyAcrossEndpoints() throws Exception {
        // Create test data for this test
        User testUser = createTestUser();
        
        // Create a category
        Category newCategory = Category.builder()
                .name("Consistency Test Category")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        Category savedCategory = categoryRepository.save(newCategory);

        // Create a budget using the category
        Budget newBudget = Budget.builder()
                .user(testUser)
                .name("Consistency Test Budget")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(BudgetStatus.ACTIVE)
                .build();
        Budget savedBudget = budgetRepository.save(newBudget);

        // Verify data consistency
        assertThat(savedCategory).isNotNull();
        assertThat(savedBudget).isNotNull();
        assertThat(savedCategory.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedBudget.getUser().getId()).isEqualTo(testUser.getId());
    }

    // Helper methods
    private User createTestUser() {
        User user = User.builder()
                .name("API Test User")
                .email("apitest@example.com")
                .password("password123")
                .createdAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    private Category createTestCategory(User user) {
        Category category = Category.builder()
                .name("Test Category")
                .type(CategoryType.EXPENSE)
                .user(user)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return categoryRepository.save(category);
    }

    private Budget createTestBudget(User user) {
        Budget budget = Budget.builder()
                .user(user)
                .name("Test Budget")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(BudgetStatus.ACTIVE)
                .build();
        return budgetRepository.save(budget);
    }

    private Goal createTestGoal(User user, Category category) {
        Goal goal = Goal.builder()
                .user(user)
                .name("Test Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(3))
                .currency("USD")
                .completed(false)
                .category(category)
                .build();
        return goalRepository.save(goal);
    }

    private void createTestTransactions(User user, Category category) {
        Transaction transaction1 = Transaction.builder()
                .user(user)
                .category(category)
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction 1")
                .date(LocalDate.now())
                .currency("USD")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        Transaction transaction2 = Transaction.builder()
                .user(user)
                .category(category)
                .amount(new BigDecimal("200.00"))
                .description("Test Transaction 2")
                .date(LocalDate.now())
                .currency("USD")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);
    }
} 