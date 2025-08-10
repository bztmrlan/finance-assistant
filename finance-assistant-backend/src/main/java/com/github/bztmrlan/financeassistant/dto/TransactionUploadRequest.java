package com.github.bztmrlan.financeassistant.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TransactionUploadRequest {
    private MultipartFile file;
    private String currency = "USD";
    private boolean autoCategorize = true;
    private boolean skipDuplicates = true;
    private String dateFormat = "yyyy-MM-dd";
} 