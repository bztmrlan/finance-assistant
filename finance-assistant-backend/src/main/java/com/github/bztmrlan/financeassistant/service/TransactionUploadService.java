package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.RawTransactionData;
import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionUploadService {

    private final FileParsingService fileParsingService;
    private final OpenAICategorizationService openAICategorizationService;
    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;

    /**
     * Main method to process transaction file upload
     */
    public TransactionUploadResponse uploadTransactions(MultipartFile file, User user, 
                                                     String currency, boolean autoCategorize, 
                                                     boolean skipDuplicates, String dateFormat) {
        long startTime = System.currentTimeMillis();
        
        try {

            List<RawTransactionData> rawTransactions = fileParsingService.parseFile(file, dateFormat, currency);
            
            if (rawTransactions.isEmpty()) {
                return TransactionUploadResponse.builder()
                        .totalRows(0)
                        .successfulTransactions(0)
                        .failedTransactions(0)
                        .skippedDuplicates(0)
                        .errors(List.of("No valid transactions found in file"))
                        .warnings(List.of())
                        .processingTime(formatProcessingTime(System.currentTimeMillis() - startTime))
                        .build();
            }


            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            int successful = 0;
            int failed = 0;
            int skipped = 0;

            for (RawTransactionData rawTransaction : rawTransactions) {
                try {
                    if (skipDuplicates && isDuplicateTransaction(rawTransaction, user)) {
                        skipped++;
                        warnings.add(String.format("Row %d: Duplicate transaction skipped", rawTransaction.getRowNumber()));
                        continue;
                    }

                    Transaction transaction = createTransaction(rawTransaction, user, autoCategorize);
                    if (transaction != null) {
                        transactionRepository.save(transaction);
                        successful++;
                        

                        ruleEngineService.evaluateRulesForTransaction(transaction);
                        
                        log.debug("Transaction saved successfully: {}", transaction.getId());
                    } else {
                        failed++;
                        errors.add(String.format("Row %d: Failed to create transaction", rawTransaction.getRowNumber()));
                    }
                    
                } catch (Exception e) {
                    failed++;
                    errors.add(String.format("Row %d: %s", rawTransaction.getRowNumber(), e.getMessage()));
                    log.error("Error processing transaction at row {}: {}", rawTransaction.getRowNumber(), e.getMessage());
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            
            return TransactionUploadResponse.builder()
                    .totalRows(rawTransactions.size())
                    .successfulTransactions(successful)
                    .failedTransactions(failed)
                    .skippedDuplicates(skipped)
                    .errors(errors)
                    .warnings(warnings)
                    .processingTime(formatProcessingTime(processingTime))
                    .build();

        } catch (IOException e) {
            log.error("Error parsing file: {}", e.getMessage());
            return TransactionUploadResponse.builder()
                    .totalRows(0)
                    .successfulTransactions(0)
                    .failedTransactions(1)
                    .skippedDuplicates(0)
                    .errors(List.of("File parsing error: " + e.getMessage()))
                    .warnings(List.of())
                    .processingTime(formatProcessingTime(System.currentTimeMillis() - startTime))
                    .build();
        }
    }

    /**
     * Creates a Transaction entity from raw data
     */
    private Transaction createTransaction(RawTransactionData rawData, User user, boolean autoCategorize) {
        try {

            Category category = null;
            if (autoCategorize && rawData.getDescription() != null && !rawData.getDescription().isEmpty()) {
                category = categorizeTransaction(rawData, user);
            } else if (rawData.getCategory() != null && !rawData.getCategory().isEmpty()) {
                category = findOrCreateCategoryByName(rawData.getCategory(), user);
            }


            CategoryType transactionType = determineTransactionType(rawData.getType(), rawData.getAmount());

            return Transaction.builder()
                    .user(user)
                    .category(category)
                    .date(rawData.getDate())
                    .amount(rawData.getAmount())
                    .currency(rawData.getCurrency())
                    .description(rawData.getDescription())
                    .build();

        } catch (Exception e) {
            log.error("Error creating transaction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Uses OpenAI to categorize a transaction
     */
    private Category categorizeTransaction(RawTransactionData rawData, User user) {
        try {
            CategoryType transactionType = determineTransactionType(rawData.getType(), rawData.getAmount());
            
            Optional<Category> categorized = openAICategorizationService.categorizeTransaction(
                    rawData.getDescription(),
                    rawData.getAmount(),
                    transactionType,
                    user
            );
            
            return categorized.orElse(null);
            
        } catch (Exception e) {
            log.warn("OpenAI categorization failed for transaction: {}", rawData.getDescription(), e);
            return null;
        }
    }

    /**
     * Finds or creates a category by name
     */
    private Category findOrCreateCategoryByName(String categoryName, User user) {

        CategoryType type = determineCategoryTypeFromName(categoryName);
        
        return Category.builder()
                .name(categoryName)
                .type(type)
                .user(user)
                .build();
    }

    /**
     * Determines the transaction type based on the type string and amount
     */
    private CategoryType determineTransactionType(String type, BigDecimal amount) {
        if (type == null) {
            return amount.compareTo(BigDecimal.ZERO) >= 0 ? CategoryType.INCOME : CategoryType.EXPENSE;
        }
        
        String lowerType = type.toLowerCase();
        
        if (lowerType.contains("income") || lowerType.contains("salary") || 
            lowerType.contains("deposit") || lowerType.contains("credit")) {
            return CategoryType.INCOME;
        } else if (lowerType.contains("transfer") || lowerType.contains("atm") || 
                   lowerType.contains("withdrawal") || lowerType.contains("move")) {
            return CategoryType.TRANSFER;
        } else {
            return CategoryType.EXPENSE;
        }
    }

    /**
     * Determines category type from category name
     */
    private CategoryType determineCategoryTypeFromName(String categoryName) {
        if (categoryName == null) {
            return CategoryType.EXPENSE;
        }
        
        String lowerName = categoryName.toLowerCase();
        
        if (lowerName.contains("income") || lowerName.contains("salary") || 
            lowerName.contains("freelance") || lowerName.contains("investment")) {
            return CategoryType.INCOME;
        } else if (lowerName.contains("transfer") || lowerName.contains("atm") || 
                   lowerName.contains("withdrawal")) {
            return CategoryType.TRANSFER;
        } else {
            return CategoryType.EXPENSE;
        }
    }

    /**
     * Checks if a transaction is a duplicate
     */
    private boolean isDuplicateTransaction(RawTransactionData rawData, User user) {
        List<Transaction> existingTransactions = transactionRepository.findByUserIdAndDateAndAmountAndDescription(
                user.getId(), rawData.getDate(), rawData.getAmount(), rawData.getDescription());
        
        return !existingTransactions.isEmpty();
    }

    /**
     * Formats processing time in a human-readable format
     */
    private String formatProcessingTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else {
            return String.format("%.2fs", milliseconds / 1000.0);
        }
    }
} 