package com.rulex.web;

public class NamedRuleNotFoundException extends RuntimeException {
    public NamedRuleNotFoundException(String name) {
        super("Named rule not found: " + name);
    }
}
