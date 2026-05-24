package com.rulex.exception;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(Long id) {
        super("Rule not found: " + id);
    }
}
