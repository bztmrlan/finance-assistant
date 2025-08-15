package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.TransactionUploadResponse;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import com.github.bztmrlan.financeassistant.service.TransactionUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions/upload")
@RequiredArgsConstructor
@Slf4j
public class TransactionUploadController {

    private final TransactionUploadService transactionUploadService;
    private final UserRepository userRepository;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionUploadResponse> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "currency", defaultValue = "USD") String currency,
            @RequestParam(value = "autoCategorize", defaultValue = "true") boolean autoCategorize,
            @RequestParam(value = "skipDuplicates", defaultValue = "true") boolean skipDuplicates,
            @RequestParam(value = "dateFormat", defaultValue = "yyyy-MM-dd") String dateFormat,
            Authentication authentication) {
        
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

            User user = getAuthenticatedUser(authentication);
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


    @GetMapping("/status")
    public ResponseEntity<String> getUploadStatus() {
        return ResponseEntity.ok("Transaction upload service is running");
    }


    @GetMapping("/formats")
    public ResponseEntity<String[]> getSupportedFormats() {
        String[] formats = {".csv", ".xlsx", ".xls"};
        return ResponseEntity.ok(formats);
    }


    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        UUID userId = null;
        

        if (principal instanceof com.github.bztmrlan.financeassistant.security.CustomUserDetailsService.CustomUserDetails) {
            userId = ((com.github.bztmrlan.financeassistant.security.CustomUserDetailsService.CustomUserDetails) principal).getUserId();
        }

        else if (principal instanceof String) {
            try {
                userId = UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format in authentication principal: {}", principal);
                return null;
            }
        }

        else if (principal instanceof UUID) {
            userId = (UUID) principal;
        }
        else {
            log.error("Unsupported authentication principal type: {}", 
                principal.getClass().getSimpleName());
            return null;
        }
        
        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        
        return null;
    }
} 