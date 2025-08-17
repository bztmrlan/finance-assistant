package com.github.bztmrlan.financeassistant.dto;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private CategoryType type;
    private LocalDateTime createdAt;
    private UUID userId;
}
