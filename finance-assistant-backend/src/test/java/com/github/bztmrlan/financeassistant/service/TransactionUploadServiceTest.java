package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.RawTransactionData;
import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionUploadServiceTest {

    @Mock
    private FileParsingService fileParsingService;

    @Mock
    private OpenAICategorizationService openAICategorizationService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @InjectMocks
    private TransactionUploadService transactionUploadService;

    private User testUser;
    private RawTransactionData testRawTransaction;
    private Category testCategory;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .build();

        testRawTransaction = RawTransactionData.builder()
                .date(LocalDate.of(2024, 1, 15))
                .amount(new BigDecimal("-25.50"))
                .type("purchase")
                .description("Starbucks Coffee")
                .category("Food & Dining")
                .currency("USD")
                .rowNumber(1)
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .date(LocalDate.of(2024, 1, 15))
                .amount(new BigDecimal("-25.50"))
                .currency("USD")
                .description("Starbucks Coffee")
                .build();
    }

    @Test
    void testUploadTransactions_Success() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "test content".getBytes()
        );

        when(fileParsingService.parseFile(any(), anyString(), anyString()))
                .thenReturn(List.of(testRawTransaction));
        when(transactionRepository.findByUserIdAndDateAndAmountAndDescription(
                any(), any(), any(), anyString())).thenReturn(List.of());
        when(openAICategorizationService.categorizeTransaction(
                anyString(), any(), any(), any())).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any())).thenReturn(testTransaction);


        TransactionUploadResponse response = transactionUploadService.uploadTransactions(
                file, testUser, "USD", true, true, "yyyy-MM-dd"
        );


        assertNotNull(response);
        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getSuccessfulTransactions());
        assertEquals(0, response.getFailedTransactions());
        assertEquals(0, response.getSkippedDuplicates());
        assertFalse(response.hasErrors());

        verify(ruleEngineService).evaluateRulesForTransaction(any());
    }

    @Test
    void testUploadTransactions_EmptyFile() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "test content".getBytes()
        );

        when(fileParsingService.parseFile(any(), anyString(), anyString()))
                .thenReturn(List.of());


        TransactionUploadResponse response = transactionUploadService.uploadTransactions(
                file, testUser, "USD", true, true, "yyyy-MM-dd"
        );


        assertNotNull(response);
        assertEquals(0, response.getTotalRows());
        assertEquals(0, response.getSuccessfulTransactions());
        assertEquals(0, response.getFailedTransactions());
        assertTrue(response.hasErrors());
        assertTrue(response.getErrors().contains("No valid transactions found in file"));
    }

    @Test
    void testUploadTransactions_DuplicateTransaction() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "test content".getBytes()
        );

        when(fileParsingService.parseFile(any(), anyString(), anyString()))
                .thenReturn(List.of(testRawTransaction));
        when(transactionRepository.findByUserIdAndDateAndAmountAndDescription(
                any(), any(), any(), anyString())).thenReturn(List.of(testTransaction));


        TransactionUploadResponse response = transactionUploadService.uploadTransactions(
                file, testUser, "USD", true, true, "yyyy-MM-dd"
        );


        assertNotNull(response);
        assertEquals(1, response.getTotalRows());
        assertEquals(0, response.getSuccessfulTransactions());
        assertEquals(0, response.getFailedTransactions());
        assertEquals(1, response.getSkippedDuplicates());
        assertTrue(response.hasWarnings());
        assertTrue(response.getWarnings().get(0).contains("Duplicate transaction skipped"));
    }

    @Test
    void testUploadTransactions_ParsingError() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "test content".getBytes()
        );

        when(fileParsingService.parseFile(any(), anyString(), anyString()))
                .thenThrow(new IOException("Invalid file format"));


        TransactionUploadResponse response = transactionUploadService.uploadTransactions(
                file, testUser, "USD", true, true, "yyyy-MM-dd"
        );


        assertNotNull(response);
        assertEquals(0, response.getTotalRows());
        assertEquals(0, response.getSuccessfulTransactions());
        assertEquals(1, response.getFailedTransactions());
        assertTrue(response.hasErrors());
        assertTrue(response.getErrors().get(0).contains("File parsing error"));
    }
} 