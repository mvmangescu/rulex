package com.rulex.exception;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String name) {
        super("Rule not found: " + name);
    }
}
