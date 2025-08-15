package com.github.bztmrlan.financeassistant.model;

import com.github.bztmrlan.financeassistant.enums.InsightType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insights")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "insight_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(nullable = false)
    private boolean viewed = false;
    
    // New fields for enhanced insights
    @Column(name = "user_question", columnDefinition = "TEXT")
    private String userQuestion;
    
    @Column(name = "insight_data", columnDefinition = "TEXT")
    private String insightData; // JSON string with supporting data
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "category_tags", columnDefinition = "TEXT")
    private String categoryTags;
    @Column(name = "time_period", columnDefinition = "TEXT")
    private String timePeriod;

}
