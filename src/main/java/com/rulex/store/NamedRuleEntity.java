package com.rulex.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "named_rules")
class NamedRuleEntity {

    @Id
    @Column(length = 256)
    private String name;

    @Column(nullable = false, length = 4096)
    private String expression;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected NamedRuleEntity() {}

    NamedRuleEntity(String name, String expression, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.expression = expression;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    String getName()       { return name; }
    String getExpression() { return expression; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }

    void setExpression(String expression) { this.expression = expression; }
    void setUpdatedAt(Instant updatedAt)  { this.updatedAt = updatedAt; }

    NamedRule toDomain() {
        return new NamedRule(name, expression, createdAt, updatedAt);
    }
}
