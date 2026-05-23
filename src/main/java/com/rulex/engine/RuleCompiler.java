package com.rulex.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.config.RuleEngineProperties;
import com.rulex.exception.RuleParseException;
import com.rulex.grammar.RuleLexer;
import com.rulex.grammar.RuleParser;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuleCompiler {

    private final Cache<String, CompiledRule> ruleCache;

    private final RuleEngineProperties properties;

    public RuleCompiler(Cache<String, CompiledRule> ruleCache, RuleEngineProperties properties) {
        this.ruleCache = ruleCache;
        this.properties = properties;
    }

    public CompiledRule compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new RuleParseException(expression, 0, 0, "Expression must not be blank");
        }
        if (expression.length() > properties.maxExpressionLength()) {
            throw new RuleParseException(expression, 0, 0,
                    "Expression exceeds maximum length of " + properties.maxExpressionLength() + " characters");
        }

        CompiledRule cached = ruleCache.getIfPresent(expression);
        if (cached != null) {
            log.debug("Cache hit — expression length={}", expression.length());
            return cached;
        }

        log.debug("Compiling expression, length={}", expression.length());
        CompiledRule compiled = doParse(expression);
        ruleCache.put(expression, compiled);
        return compiled;
    }

    public void validate(String expression) {
        compile(expression);
    }

    private CompiledRule doParse(String expression) {
        RuleLexer lexer = new RuleLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener(expression));

        RuleParser parser = new RuleParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener(expression));

        return new CompiledRule(expression, parser.program());
    }

    private static class ThrowingErrorListener extends BaseErrorListener {

        private final String expression;

        ThrowingErrorListener(String expression) {
            this.expression = expression;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuleParseException(expression, line, charPositionInLine, msg);
        }
    }
}
