package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.CategoryRequest;
import com.github.bztmrlan.financeassistant.dto.CategoryResponse;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.exception.AuthenticationException;
import com.github.bztmrlan.financeassistant.exception.ResourceNotFoundException;
import com.github.bztmrlan.financeassistant.exception.ValidationException;
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
            throw new AuthenticationException("Authentication is required");
        }
        
        Object principal = authentication.getPrincipal();

        return ((CustomUserDetailsService.CustomUserDetails) principal).getUserId();
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getUserCategories(Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        List<Category> categories = categoryRepository.findByUserId(userId);
        
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::convertToCategoryResponse)
                .toList();
        
        return ResponseEntity.ok(categoryResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id, Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        
        return ResponseEntity.ok(convertToCategoryResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request, Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        if (categoryRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new ValidationException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .type(request.getType())
                .user(User.builder().id(userId).build())
                .createdAt(LocalDateTime.now())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(convertToCategoryResponse(savedCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @RequestBody CategoryRequest request, Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        Category existingCategory = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        
        existingCategory.setName(request.getName());
        existingCategory.setType(request.getType());
        Category savedCategory = categoryRepository.save(existingCategory);
        
        return ResponseEntity.ok(convertToCategoryResponse(savedCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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