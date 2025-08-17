package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.RawTransactionData;
import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionUploadServiceTest {

    @Mock
    private FileParsingService fileParsingService;

    @Mock
    private HuggingFaceCategorizationService huggingFaceCategorizationService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private BudgetManagementService budgetManagementService;

    @Mock
    private GoalManagementService goalManagementService;

    @InjectMocks
    private TransactionUploadService transactionUploadService;

    private UUID testUserId;
    private UUID testCategoryId;
    private User testUser;
    private Category testCategory;
    private RawTransactionData testRawTransaction;
    private Transaction testTransaction;
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() throws IOException {
        testUserId = UUID.randomUUID();
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
        
        testRawTransaction = RawTransactionData.builder()
                .description("Test Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .description("Test Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .user(testUser)
                .build();
        
        testFile = new MockMultipartFile(
                "file",
                "test-transactions.csv",
                "text/csv",
                "test,data,content".getBytes()
        );
    }

    // ==================== HAPPY PATH TESTS ====================


    @Test
    void testUploadTransactions_WithAutoCategorization() throws IOException {
        // Given
        RawTransactionData rawTransaction = RawTransactionData.builder()
                .description("Test Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category(null) // No category, should trigger auto-categorization
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(rawTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(huggingFaceCategorizationService.categorizeTransaction(anyString(), any(BigDecimal.class), any(CategoryType.class), any(User.class)))
                .thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", true, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        // The service determines category type based on amount, so we need to verify with the correct type
        verify(huggingFaceCategorizationService).categorizeTransaction(eq("Test Transaction"), eq(new BigDecimal("100.00")), any(CategoryType.class), eq(testUser));
    }

    @Test
    void testUploadTransactions_SkipDuplicates() throws IOException {
        // Given
        List<RawTransactionData> rawTransactions = List.of(testRawTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.findByUserIdAndDateAndAmountAndDescription(
                any(UUID.class), any(LocalDate.class), any(BigDecimal.class), anyString()))
                .thenReturn(List.of(testTransaction));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, true, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(0, result.getFailedTransactions());
        assertEquals(1, result.getSkippedDuplicates());
        
        verify(transactionRepository, never()).saveAll(any());
    }



    // ==================== UNHAPPY PATH TESTS ====================

    @Test
    void testUploadTransactions_EmptyFile() throws IOException {
        // Given
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(List.of());

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(0, result.getFailedTransactions());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("No valid transactions found"));
        
        verify(transactionRepository, never()).saveAll(any());
        verify(ruleEngineService, never()).evaluateRulesForUser(any());
    }

    @Test
    void testUploadTransactions_ParsingError() throws IOException {
        // Given
        when(fileParsingService.parseFile(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("File parsing failed"));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(1, result.getFailedTransactions());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Failed to process file"));
        
        verify(transactionRepository, never()).saveAll(any());
        verify(ruleEngineService, never()).evaluateRulesForUser(any());
    }

    @Test
    void testUploadTransactions_InvalidTransactionData() throws IOException {
        // Given
        RawTransactionData invalidTransaction = RawTransactionData.builder()
                .description("") // Empty description
                .amount(null) // Null amount
                .date(null) // Null date
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(invalidTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(1, result.getFailedTransactions());
        assertFalse(result.getErrors().isEmpty());
        
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void testUploadTransactions_DatabaseError() throws IOException {
        // Given
        List<RawTransactionData> rawTransactions = List.of(testRawTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Database connection failed"));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(1, result.getFailedTransactions());
        assertFalse(result.getErrors().isEmpty());
        
        verify(transactionRepository).saveAll(any());
    }



    // ==================== EDGE CASE TESTS ====================

    @Test
    void testUploadTransactions_ZeroAmount() throws IOException {
        // Given
        RawTransactionData zeroAmountTransaction = RawTransactionData.builder()
                .description("Zero Amount Transaction")
                .amount(BigDecimal.ZERO)
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(zeroAmountTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_NegativeAmount() throws IOException {
        // Given
        RawTransactionData negativeAmountTransaction = RawTransactionData.builder()
                .description("Negative Amount Transaction")
                .amount(new BigDecimal("-50.00"))
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(negativeAmountTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_VeryLargeAmount() throws IOException {
        // Given
        RawTransactionData largeAmountTransaction = RawTransactionData.builder()
                .description("Large Amount Transaction")
                .amount(new BigDecimal("999999999.99"))
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(largeAmountTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_SpecialCharacters() throws IOException {
        // Given
        RawTransactionData specialCharTransaction = RawTransactionData.builder()
                .description("Transaction with special chars: !@#$%^&*()")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category("Category with spaces & symbols!")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(specialCharTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_LongDescription() throws IOException {
        // Given
        String longDescription = "A".repeat(1000); // Very long description
        RawTransactionData longDescTransaction = RawTransactionData.builder()
                .description(longDescription)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(longDescTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_FutureDate() throws IOException {
        // Given
        RawTransactionData futureDateTransaction = RawTransactionData.builder()
                .description("Future Date Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now().plusDays(30)) // Future date
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(futureDateTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_PastDate() throws IOException {
        // Given
        RawTransactionData pastDateTransaction = RawTransactionData.builder()
                .description("Past Date Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now().minusDays(365)) // Past date
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(pastDateTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessfulTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_MixedSuccessAndFailure() throws IOException {
        // Given
        RawTransactionData validTransaction = RawTransactionData.builder()
                .description("Valid Transaction")
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category("Test Category")
                .rowNumber(0)
                .build();
        
        RawTransactionData invalidTransaction = RawTransactionData.builder()
                .description("Invalid Transaction")
                .amount(null) // Null amount - this will make it invalid
                .date(null) // Null date - this will make it invalid
                .category("Test Category")
                .rowNumber(1)
                .build();
        
        List<RawTransactionData> rawTransactions = List.of(validTransaction, invalidTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessfulTransactions());
        assertEquals(1, result.getFailedTransactions());
        
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void testUploadTransactions_ProcessingTimeCalculation() throws IOException {
        // Given
        List<RawTransactionData> rawTransactions = List.of(testRawTransaction);
        when(fileParsingService.parseFile(any(), anyString(), anyString())).thenReturn(rawTransactions);
        when(transactionRepository.saveAll(any())).thenReturn(List.of(testTransaction));
        when(categoryRepository.findByNameAndUserId(anyString(), any(UUID.class))).thenReturn(Optional.of(testCategory));

        // When
        TransactionUploadResponse result = transactionUploadService.uploadTransactions(
                testFile, testUser, "USD", false, false, "yyyy-MM-dd");

        // Then
        assertNotNull(result);
        assertNotNull(result.getProcessingTime());
        assertTrue(result.getProcessingTime().contains("ms") || result.getProcessingTime().contains("s"));
        
        verify(transactionRepository).saveAll(any());
    }
} 