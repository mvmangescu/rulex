package com.rulex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public RuleEntity(String name, String expression, String description) {
        this.name = name;
        this.expression = expression;
        this.description = description;
    }
}
