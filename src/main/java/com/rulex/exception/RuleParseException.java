package com.rulex.exception;

import lombok.Getter;

@Getter
public class RuleParseException extends RuntimeException {

    private final String expression;
    private final int line;
    private final int charPosition;

    public RuleParseException(String expression, int line, int charPosition, String msg) {
        super(String.format("Parse error in expression [%s] at line %d:%d — %s",
                expression, line, charPosition, msg));
        this.expression = expression;
        this.line = line;
        this.charPosition = charPosition;
    }
}
