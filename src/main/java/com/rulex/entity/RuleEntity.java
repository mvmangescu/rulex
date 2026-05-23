package com.rulex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 256)
    private String name;

    @Column(nullable = false, length = 4096)
    private String expression;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public RuleEntity(String name, String expression, String description, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.expression = expression;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
