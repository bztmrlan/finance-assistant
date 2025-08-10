package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.service.TransactionUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/upload")
@RequiredArgsConstructor
@Slf4j
public class TransactionUploadController {

    private final TransactionUploadService transactionUploadService;

    /**
     * Upload transactions from CSV or Excel file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionUploadResponse> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "currency", defaultValue = "USD") String currency,
            @RequestParam(value = "autoCategorize", defaultValue = "true") boolean autoCategorize,
            @RequestParam(value = "skipDuplicates", defaultValue = "true") boolean skipDuplicates,
            @RequestParam(value = "dateFormat", defaultValue = "yyyy-MM-dd") String dateFormat) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    TransactionUploadResponse.builder()
                        .totalRows(0)
                        .successfulTransactions(0)
                        .failedTransactions(1)
                        .skippedDuplicates(0)
                        .errors(List.of("File is empty"))
                        .warnings(List.of())
                        .processingTime("0ms")
                        .build()
                );
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".csv") && 
                                   !fileName.toLowerCase().endsWith(".xlsx") && 
                                   !fileName.toLowerCase().endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(
                    TransactionUploadResponse.builder()
                        .totalRows(0)
                        .successfulTransactions(0)
                        .failedTransactions(1)
                        .skippedDuplicates(0)
                        .errors(List.of("Unsupported file format. Please use CSV or Excel files."))
                        .warnings(List.of())
                        .processingTime("0ms")
                        .build()
                );
            }


            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body(
                    TransactionUploadResponse.builder()
                        .totalRows(0)
                        .successfulTransactions(0)
                        .failedTransactions(1)
                        .skippedDuplicates(0)
                        .errors(List.of("User not authenticated"))
                        .warnings(List.of())
                        .processingTime("0ms")
                        .build()
                );
            }

            log.info("Processing transaction upload for user: {}, file: {}, size: {} bytes", 
                    user.getId(), fileName, file.getSize());


            TransactionUploadResponse response = transactionUploadService.uploadTransactions(
                    file, user, currency, autoCategorize, skipDuplicates, dateFormat);

            log.info("Transaction upload completed for user: {}. Success: {}, Failed: {}, Skipped: {}", 
                    user.getId(), response.getSuccessfulTransactions(), 
                    response.getFailedTransactions(), response.getSkippedDuplicates());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing transaction upload", e);
            return ResponseEntity.internalServerError().body(
                TransactionUploadResponse.builder()
                    .totalRows(0)
                    .successfulTransactions(0)
                    .failedTransactions(1)
                    .skippedDuplicates(0)
                    .errors(List.of("Internal server error: " + e.getMessage()))
                    .warnings(List.of())
                    .processingTime("0ms")
                    .build()
            );
        }
    }

    /**
     * Get upload status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<String> getUploadStatus() {
        return ResponseEntity.ok("Transaction upload service is running");
    }

    /**
     * Get supported file formats
     */
    @GetMapping("/formats")
    public ResponseEntity<String[]> getSupportedFormats() {
        String[] formats = {".csv", ".xlsx", ".xls"};
        return ResponseEntity.ok(formats);
    }

    /**
     * Get expected CSV/Excel column structure
     */
    @GetMapping("/template")
    public ResponseEntity<String> getTemplate() {
        String template = """
            Expected CSV/Excel format:
            
            Column 1: Date (format: yyyy-MM-dd)
            Column 2: Amount (positive for income, negative for expenses)
            Column 3: Type (optional: purchase, transfer, income, etc.)
            Column 4: Description (optional: transaction description)
            Column 5: Category (optional: pre-defined category)
            
            Example:
            Date,Amount,Type,Description,Category
            2024-01-15,-25.50,purchase,Starbucks Coffee,Food & Dining
            2024-01-16,1200.00,income,Salary Payment,Income
            2024-01-17,-45.00,transfer,ATM Withdrawal,Transfer
            """;
        
        return ResponseEntity.ok(template);
    }

    /**
     * Helper method to get authenticated user
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
} 