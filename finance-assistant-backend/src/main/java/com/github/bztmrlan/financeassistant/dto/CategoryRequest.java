package com.github.bztmrlan.financeassistant.dto;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import lombok.Data;

@Data
public class CategoryRequest {
    private String name;
    private CategoryType type;
}
