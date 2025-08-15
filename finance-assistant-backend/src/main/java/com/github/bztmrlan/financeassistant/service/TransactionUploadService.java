package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.RawTransactionData;
import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionUploadService {

    private final FileParsingService fileParsingService;
    private final HuggingFaceCategorizationService huggingFaceCategorizationService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final RuleEngineService ruleEngineService;
    private final BudgetManagementService budgetManagementService;
    private final GoalManagementService goalManagementService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);


    public TransactionUploadResponse uploadTransactions(MultipartFile file, User user, String currency,
                                                     boolean autoCategorize, boolean skipDuplicates, String dateFormat) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            log.info("Starting transaction upload for user: {}, file: {}", user.getId(), file.getOriginalFilename());
            

            List<RawTransactionData> rawTransactions = fileParsingService.parseFile(file, dateFormat, currency);
            
            if (rawTransactions.isEmpty()) {
                return TransactionUploadResponse.builder()
                    .totalRows(0)
                    .successfulTransactions(0)
                    .failedTransactions(0)
                    .skippedDuplicates(0)
                    .errors(List.of("No valid transactions found in file"))
                    .warnings(warnings)
                    .processingTime(calculateProcessingTime(startTime))
                    .build();
            }


            TransactionProcessingResult result = processTransactions(rawTransactions, user, autoCategorize, skipDuplicates);
            

            if (result.getSuccessfulTransactions() > 0) {
                applyBusinessRules(result.getTransactions(), user);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Transaction upload completed. Success: {}, Failed: {}, Skipped: {}, Time: {}ms",
                    result.getSuccessfulTransactions(), result.getFailedTransactions(), 
                    result.getSkippedDuplicates(), processingTime);

            return TransactionUploadResponse.builder()
                .totalRows(rawTransactions.size())
                .successfulTransactions(result.getSuccessfulTransactions())
                .failedTransactions(result.getFailedTransactions())
                .skippedDuplicates(result.getSkippedDuplicates())
                .errors(result.getErrors())
                .warnings(result.getWarnings())
                .processingTime(calculateProcessingTime(startTime))
                .build();

        } catch (Exception e) {
            log.error("Error during transaction upload", e);
            errors.add("Failed to process file: " + e.getMessage());
            
            return TransactionUploadResponse.builder()
                .totalRows(0)
                .successfulTransactions(0)
                .failedTransactions(1)
                .skippedDuplicates(0)
                .errors(errors)
                .warnings(warnings)
                .processingTime(calculateProcessingTime(startTime))
                .build();
        }
    }


    private TransactionProcessingResult processTransactions(List<RawTransactionData> rawTransactions, 
                                                         User user, boolean autoCategorize, boolean skipDuplicates) {
        List<Transaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int skippedDuplicates = 0;
        int successfulCount = 0;
        int failedCount = 0;

        Map<Integer, RawTransactionData> transactionMap = rawTransactions.stream()
            .collect(Collectors.toMap(RawTransactionData::getRowNumber, t -> t));

        for (RawTransactionData raw : rawTransactions) {
            try {
                if (skipDuplicates && isDuplicateTransaction(raw, user)) {
                    skippedDuplicates++;
                    warnings.add(String.format("Row %d: Skipped duplicate transaction", raw.getRowNumber()));
                    continue;
                }


                Optional<Transaction> transaction = processSingleTransaction(raw, user, autoCategorize);
                
                if (transaction.isPresent()) {
                    transactions.add(transaction.get());
                    successfulCount++;
                } else {
                    failedCount++;
                    errors.add(String.format("Row %d: Failed to process transaction", raw.getRowNumber()));
                }

            } catch (Exception e) {
                failedCount++;
                errors.add(String.format("Row %d: Error processing transaction: %s", raw.getRowNumber(), e.getMessage()));
                log.warn("Error processing transaction at row {}: {}", raw.getRowNumber(), e.getMessage());
            }
        }


        if (!transactions.isEmpty()) {
            try {
                transactionRepository.saveAll(transactions);
                log.info("Saved {} transactions to database", transactions.size());
                

                for (Transaction transaction : transactions) {
                    try {
                        if (transaction.getCategory() != null) {
                            List<Budget> userBudgets = budgetManagementService.getUserBudgets(user.getId());
                            for (Budget budget : userBudgets) {
                                try {
                                    budgetManagementService.updateBudgetCategorySpending(budget.getId(), transaction.getCategory().getId());
                                } catch (Exception e) {
                                    log.debug("Failed to update budget {} category {}: {}", 
                                        budget.getId(), transaction.getCategory().getId(), e.getMessage());
                                }
                            }
                        }
                        log.debug("Updated budget spending for transaction: {}", transaction.getId());
                    } catch (Exception e) {
                        log.error("Failed to update budget spending for transaction: {}", transaction.getId(), e);
                    }
                }
                

                try {
                    goalManagementService.calculateGoalProgressFromTransactions(user.getId());
                    log.info("Updated goal progress for user {} after transaction upload", user.getId());
                } catch (Exception e) {
                    log.error("Failed to update goal progress for user {}: {}", user.getId(), e.getMessage());
                }
                
            } catch (Exception e) {
                log.error("Failed to save transactions to database", e);
                errors.add("Failed to save transactions to database: " + e.getMessage());
                successfulCount = 0;
                failedCount += transactions.size();
                transactions.clear();
            }
        }

        return new TransactionProcessingResult(transactions, successfulCount, failedCount, skippedDuplicates, errors, warnings);
    }

    private Optional<Transaction> processSingleTransaction(RawTransactionData raw, User user, boolean autoCategorize) {
        try {
            if (raw.getDate() == null || raw.getAmount() == null) {
                return Optional.empty();
            }

            Category category = null;
            if (raw.getCategory() != null && !raw.getCategory().trim().isEmpty()) {
                category = findOrCreateCategory(raw.getCategory(), user);
            } else if (autoCategorize) {
                Optional<Category> aiCategory = huggingFaceCategorizationService.categorizeTransaction(
                    raw.getDescription(), raw.getAmount(), 
                    raw.getAmount().compareTo(BigDecimal.ZERO) < 0 ? CategoryType.EXPENSE : CategoryType.INCOME, 
                    user
                );
                category = aiCategory.orElse(null);
            }

            Transaction transaction = Transaction.builder()
                .user(user)
                .date(raw.getDate())
                .amount(raw.getAmount())
                .currency(raw.getCurrency())
                .description(raw.getDescription() != null ? raw.getDescription().trim() : "")
                .type(raw.getType())
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

            return Optional.of(transaction);

        } catch (Exception e) {
            log.warn("Error processing transaction: {}", e.getMessage());
            return Optional.empty();
        }
    }


    private boolean isDuplicateTransaction(RawTransactionData raw, User user) {
        if (raw.getDate() == null || raw.getAmount() == null) {
            return false;
        }

        List<Transaction> existing = transactionRepository.findByUserIdAndDateAndAmountAndDescription(
            user.getId(), raw.getDate(), raw.getAmount(), 
            raw.getDescription() != null ? raw.getDescription().trim() : ""
        );

        return !existing.isEmpty();
    }


    private Category findOrCreateCategory(String categoryName, User user) {
        Optional<Category> existing = categoryRepository.findByNameAndUserId(categoryName, user.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            CategoryType categoryType = determineCategoryType(categoryName);

            Category newCategory = Category.builder()
                    .name(categoryName)
                    .type(categoryType)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Category savedCategory = categoryRepository.save(newCategory);
            log.info("Created new category: {} for user: {}", categoryName, user.getEmail());
            return savedCategory;
            
        } catch (Exception e) {
            log.error("Error creating category: {} for user: {}", categoryName, user.getEmail(), e);
            throw new RuntimeException("Failed to create category: " + categoryName, e);
        }
    }


    private CategoryType determineCategoryType(String categoryName) {
        String lowerName = categoryName.toLowerCase();
        
        if (lowerName.contains("income") || lowerName.contains("salary") || lowerName.contains("bonus")) {
            return CategoryType.INCOME;
        } else if (lowerName.contains("transfer") || lowerName.contains("withdrawal") || lowerName.contains("deposit")) {
            return CategoryType.TRANSFER;
        } else {
            return CategoryType.EXPENSE;
        }
    }


    private void applyBusinessRules(List<Transaction> transactions, User user) {
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ruleEngineService.evaluateRulesForUser(user.getId());
                    log.info("Applied business rules for {} transactions", transactions.size());
                } catch (Exception e) {
                    log.error("Error applying business rules", e);
                }
            }, executorService);
        } catch (Exception e) {
            log.warn("Could not schedule business rules evaluation", e);
        }
    }


    private String calculateProcessingTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration < 1000) {
            return duration + "ms";
        } else {
            return String.format("%.2fs", duration / 1000.0);
        }
    }


    public void shutdown() {
        executorService.shutdown();
    }

    @Getter
    @AllArgsConstructor
    private static class TransactionProcessingResult {
        private final List<Transaction> transactions;
        private final int successfulTransactions;
        private final int failedTransactions;
        private final int skippedDuplicates;
        private final List<String> errors;
        private final List<String> warnings;
    }
} 