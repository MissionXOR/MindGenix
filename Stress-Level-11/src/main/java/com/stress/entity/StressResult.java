package com.stress.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
public class StressResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user; // Links this result to a specific user

    @ElementCollection
    private Map<String, Integer> responses; // Key: Question ID, Value: Selected Score

    private double physicalStressPercentage;
    private String physicalStressLevel; // High, Medium, Low
    private double mentalStressPercentage;
    private String mentalStressLevel; // High, Medium, Low
    private double jobStressPercentage;
    private String jobStressLevel; // High, Medium, Low

    private String physicalSuggestion;
    private String mentalSuggestion;
    private String jobSuggestion;

    private LocalDateTime createdAt;

    public StressResult() {
        this.createdAt = LocalDateTime.now();
    }
}