package com.stress.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventCode;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}