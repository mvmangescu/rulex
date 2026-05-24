package com.rulex.exception;

import lombok.Getter;

@Getter
public class RuleNotFoundException extends RuntimeException {

    private final Long id;

    public RuleNotFoundException(Long id) {
        super("Rule not found: " + id);
        this.id = id;
    }
}
