package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.CategoryRequest;
import com.github.bztmrlan.financeassistant.dto.CategoryResponse;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.exception.AuthenticationException;
import com.github.bztmrlan.financeassistant.exception.ResourceNotFoundException;
import com.github.bztmrlan.financeassistant.exception.ValidationException;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetailsService.CustomUserDetails customUserDetails;

    @InjectMocks
    private CategoryController categoryController;

    private UUID testUserId;
    private UUID testCategoryId;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .build();
        
        testCategory = Category.builder()
                .id(testCategoryId)
                .name("Test Category")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    void testGetUserCategories_Success() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByUserId(testUserId)).thenReturn(List.of(testCategory));

        // When
        ResponseEntity<List<CategoryResponse>> response = categoryController.getUserCategories(authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        CategoryResponse categoryResponse = response.getBody().get(0);
        assertEquals(testCategoryId, categoryResponse.getId());
        assertEquals("Test Category", categoryResponse.getName());
        assertEquals(CategoryType.EXPENSE, categoryResponse.getType());
        assertEquals(testUserId, categoryResponse.getUserId());

        verify(categoryRepository).findByUserId(testUserId);
    }

    @Test
    void testGetCategory_Success() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.of(testCategory));

        // When
        ResponseEntity<CategoryResponse> response = categoryController.getCategory(testCategoryId, authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        CategoryResponse categoryResponse = response.getBody();
        assertEquals(testCategoryId, categoryResponse.getId());
        assertEquals("Test Category", categoryResponse.getName());
        assertEquals(CategoryType.EXPENSE, categoryResponse.getType());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
    }

    @Test
    void testCreateCategory_Success() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category");
        request.setType(CategoryType.INCOME);

        Category savedCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("New Category")
                .type(CategoryType.INCOME)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.existsByNameAndUserId("New Category", testUserId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        ResponseEntity<CategoryResponse> response = categoryController.createCategory(request, authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        CategoryResponse categoryResponse = response.getBody();
        assertEquals("New Category", categoryResponse.getName());
        assertEquals(CategoryType.INCOME, categoryResponse.getType());

        verify(categoryRepository).existsByNameAndUserId("New Category", testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_Success() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Category");
        request.setType(CategoryType.INCOME);

        Category existingCategory = Category.builder()
                .id(testCategoryId)
                .name("Old Name")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // When
        ResponseEntity<CategoryResponse> response = categoryController.updateCategory(testCategoryId, request, authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        CategoryResponse categoryResponse = response.getBody();
        assertEquals("Updated Category", categoryResponse.getName());
        assertEquals(CategoryType.INCOME, categoryResponse.getType());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void testDeleteCategory_Success() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).deleteById(testCategoryId);

        // When
        ResponseEntity<Void> response = categoryController.deleteCategory(testCategoryId, authentication);

        // Then
        assertNotNull(response);
        assertEquals(204, response.getStatusCode().value());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(categoryRepository).deleteById(testCategoryId);
    }

    // ==================== UNHAPPY PATH TESTS ====================

    @Test
    void testGetCategory_NotFound() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryController.getCategory(testCategoryId, authentication);
        });

        assertEquals("Category with identifier '" + testCategoryId + "' not found", exception.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
    }

    @Test
    void testCreateCategory_DuplicateName() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName("Existing Category");
        request.setType(CategoryType.EXPENSE);

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.existsByNameAndUserId("Existing Category", testUserId)).thenReturn(true);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            categoryController.createCategory(request, authentication);
        });

        assertEquals("Category with name 'Existing Category' already exists", exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());

        verify(categoryRepository).existsByNameAndUserId("Existing Category", testUserId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_NotFound() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Category");
        request.setType(CategoryType.INCOME);

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryController.updateCategory(testCategoryId, request, authentication);
        });

        assertEquals("Category with identifier '" + testCategoryId + "' not found", exception.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testDeleteCategory_NotFound() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryController.deleteCategory(testCategoryId, authentication);
        });

        assertEquals("Category with identifier '" + testCategoryId + "' not found", exception.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(categoryRepository, never()).deleteById(any(UUID.class));
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    void testGetUserCategories_AuthenticationRequired() {
        // Given
        when(authentication.getPrincipal()).thenReturn(null);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            categoryController.getUserCategories(authentication);
        });

        assertEquals("Authentication is required", exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());

        verify(categoryRepository, never()).findByUserId(any(UUID.class));
    }

    @Test
    void testGetCategory_AuthenticationRequired() {
        // Given
        when(authentication.getPrincipal()).thenReturn(null);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            categoryController.getCategory(testCategoryId, authentication);
        });

        assertEquals("Authentication is required", exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());

        verify(categoryRepository, never()).findByIdAndUserId(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCreateCategory_AuthenticationRequired() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category");
        request.setType(CategoryType.EXPENSE);

        when(authentication.getPrincipal()).thenReturn(null);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            categoryController.createCategory(request, authentication);
        });

        assertEquals("Authentication is required", exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());

        verify(categoryRepository, never()).existsByNameAndUserId(anyString(), any(UUID.class));
        verify(categoryRepository, never()).save(any(Category.class));
    }



    @Test
    void testGetUserCategories_EmptyList() {
        // Given
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByUserId(testUserId)).thenReturn(List.of());

        // When
        ResponseEntity<List<CategoryResponse>> response = categoryController.getUserCategories(authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(categoryRepository).findByUserId(testUserId);
    }

    @Test
    void testCreateCategory_WithSpecialCharacters() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName("Category with spaces & symbols!");
        request.setType(CategoryType.EXPENSE);

        Category savedCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Category with spaces & symbols!")
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.existsByNameAndUserId("Category with spaces & symbols!", testUserId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        ResponseEntity<CategoryResponse> response = categoryController.createCategory(request, authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        CategoryResponse categoryResponse = response.getBody();
        assertEquals("Category with spaces & symbols!", categoryResponse.getName());

        verify(categoryRepository).existsByNameAndUserId("Category with spaces & symbols!", testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_NoChanges() {
        // Given
        CategoryRequest request = new CategoryRequest();
        request.setName(testCategory.getName()); // Same name
        request.setType(testCategory.getType()); // Same type

        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUserId()).thenReturn(testUserId);
        when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // When
        ResponseEntity<CategoryResponse> response = categoryController.updateCategory(testCategoryId, request, authentication);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        CategoryResponse categoryResponse = response.getBody();
        assertEquals(testCategory.getName(), categoryResponse.getName());
        assertEquals(testCategory.getType(), categoryResponse.getType());

        verify(categoryRepository).findByIdAndUserId(testCategoryId, testUserId);
        verify(categoryRepository).save(testCategory);
    }


} 