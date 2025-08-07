package com.github.bztmrlan.financeassistant.model;

import com.github.bztmrlan.financeassistant.enums.InsightType;
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
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightType type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(nullable = false)
    private boolean viewed = false;
}
