package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID userId);
    
    List<Category> findByUserIdAndType(UUID userId, CategoryType type);
    
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    
    Optional<Category> findByNameAndUserId(String name, UUID userId);
    
    boolean existsByNameAndUserId(String name, UUID userId);

}
