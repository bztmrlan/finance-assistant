package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryRepository categoryRepository;




    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        Object principal = authentication.getPrincipal();
        

        if (principal instanceof CustomUserDetailsService.CustomUserDetails) {
            return ((CustomUserDetailsService.CustomUserDetails) principal).getUserId();
        }
        

        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID format in authentication principal: " + principal);
            }
        }
        

        if (principal instanceof UUID) {
            return (UUID) principal;
        }
        
        throw new IllegalArgumentException("Unsupported authentication principal type: " + 
            (principal != null ? principal.getClass().getSimpleName() : "null"));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getUserCategories(Authentication authentication) {
        try {

            
            if (authentication == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            if (authentication.getPrincipal() == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            UUID userId = extractUserIdFromAuthentication(authentication);

            
            List<Category> categories = categoryRepository.findByUserId(userId);


            List<CategoryResponse> categoryResponses = categories.stream()
                    .map(this::convertToCategoryResponse)
                    .toList();
            
            return ResponseEntity.ok(categoryResponses);
            
        } catch (Exception e) {
            log.error("Error retrieving categories for user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            return categoryRepository.findByIdAndUserId(id, userId)
                    .map(this::convertToCategoryResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving category {} for user: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request, Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            
            if (categoryRepository.existsByNameAndUserId(request.getName(), userId)) {
                return ResponseEntity.badRequest().build();
            }

            Category category = Category.builder()
                    .name(request.getName())
                    .type(request.getType())
                    .user(User.builder().id(userId).build())
                    .createdAt(LocalDateTime.now())
                    .build();

            Category savedCategory = categoryRepository.save(category);
            return ResponseEntity.ok(convertToCategoryResponse(savedCategory));
        } catch (Exception e) {
            log.error("Error creating category for user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @RequestBody UpdateCategoryRequest request, Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            
            return categoryRepository.findByIdAndUserId(id, userId)
                    .map(existingCategory -> {
                        existingCategory.setName(request.getName());
                        existingCategory.setType(request.getType());
                        Category savedCategory = categoryRepository.save(existingCategory);
                        return ResponseEntity.ok(convertToCategoryResponse(savedCategory));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating category {} for user: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            
            if (categoryRepository.findByIdAndUserId(id, userId).isPresent()) {
                categoryRepository.deleteById(id);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting category {} for user: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @Data
    public static class CreateCategoryRequest {
        private String name;
        private CategoryType type;
    }

    @Data
    public static class UpdateCategoryRequest {
        private String name;
        private CategoryType type;
    }

    @Data
    public static class CategoryResponse {
        private UUID id;
        private String name;
        private CategoryType type;
        private LocalDateTime createdAt;
        private UUID userId;

        public CategoryResponse(UUID id, String name, CategoryType type, LocalDateTime createdAt, UUID userId) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.createdAt = createdAt;
            this.userId = userId;
        }
    }

    private CategoryResponse convertToCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }
        
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getType(),
            category.getCreatedAt(),
            category.getUser() != null ? category.getUser().getId() : null
        );
    }
} 