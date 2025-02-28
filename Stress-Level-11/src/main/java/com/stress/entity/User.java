package com.stress.entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class User {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<StressResult> stressResults;
    // Getters and Setters
    private String profilePicture; 
}
