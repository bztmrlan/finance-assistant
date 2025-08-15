package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.dto.TransactionResponse;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;


    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            List<Transaction> transactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());

            
            List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(transactionResponses);
        } catch (Exception e) {
            log.error("Error retrieving transactions for user", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{transactionId}/category")
    public ResponseEntity<?> updateTransactionCategory(
            @PathVariable UUID transactionId,
            @RequestBody CategoryUpdateRequest request,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }


            Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId());
            if (transaction == null) {
                log.warn("Transaction {} not found or doesn't belong to user {}", transactionId, user.getId());
                return ResponseEntity.notFound().build();
            }

            Category newCategory = categoryRepository.findById(request.getCategoryId()).orElse(null);
            if (newCategory == null) {
                log.warn("Category {} not found", request.getCategoryId());
                return ResponseEntity.badRequest().body("Category not found");
            }


            if (!newCategory.getUser().getId().equals(user.getId())) {
                log.warn("Category {} doesn't belong to user {}", request.getCategoryId(), user.getId());
                return ResponseEntity.badRequest().body("Category not found");
            }


            transaction.setCategory(newCategory);
            transactionRepository.save(transaction);

            log.info("Updated category for transaction {} to category {} for user {}", 
                    transactionId, request.getCategoryId(), user.getId());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating transaction category", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID transactionId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId());
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }

            TransactionResponse response = convertToResponse(transaction);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving transaction {}", transactionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable UUID transactionId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }


            Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId());
            if (transaction == null) {
                log.warn("Transaction {} not found or doesn't belong to user {}", transactionId, user.getId());
                return ResponseEntity.notFound().build();
            }

            transactionRepository.delete(transaction);
            log.info("Deleted transaction {} for user {}", transactionId, user.getId());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting transaction {}", transactionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof CustomUserDetailsService.CustomUserDetails) {
                return userRepository.findById(((CustomUserDetailsService.CustomUserDetails) principal).getUserId()).orElse(null);
            } else if (principal instanceof String) {
                UUID userId = UUID.fromString((String) principal);
                return userRepository.findById(userId).orElse(null);
            } else if (principal instanceof UUID) {
                return userRepository.findById((UUID) principal).orElse(null);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting user from authentication", e);
            return null;
        }
    }


    private TransactionResponse convertToResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .userId(transaction.getUser().getId())
            .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
            .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
            .categoryType(transaction.getCategory() != null ? transaction.getCategory().getType().name() : null)
            .date(transaction.getDate())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .description(transaction.getDescription())
            .type(transaction.getType())
            .build();
    }

    @Data
    public static class CategoryUpdateRequest {
        private UUID categoryId;
    }
} 