package com.rulex.engine;

import com.rulex.exception.RuleEvaluationException;
import com.rulex.function.FunctionRegistry;
import com.rulex.grammar.RuleBaseVisitor;
import com.rulex.grammar.RuleLexer;
import com.rulex.grammar.RuleParser;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@Slf4j
public class RuleEvaluator extends RuleBaseVisitor<RuleValue> {

    private final EvaluationContext ctx;

    private final FunctionRegistry registry;

    private final int maxSteps;

    private int stepCount = 0;

    public RuleEvaluator(EvaluationContext ctx, FunctionRegistry registry, int maxSteps) {
        this.ctx = ctx;
        this.registry = registry;
        this.maxSteps = maxSteps;
    }

    @Override
    public RuleValue visit(ParseTree tree) {
        if (++stepCount > maxSteps) {
            throw new RuleEvaluationException(
                    "Rule evaluation exceeded complexity limit of " + maxSteps + " steps");
        }
        return super.visit(tree);
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    public RuleValue visitProgram(RuleParser.ProgramContext ctx) {
        return visit(ctx.expr());
    }

    // ── Boolean logic ─────────────────────────────────────────────────────────

    @Override
    public RuleValue visitOrExpr(RuleParser.OrExprContext ctx) {
        if (visit(ctx.expr(0)).asBoolean()) return RuleValue.TRUE;
        return visit(ctx.expr(1));
    }

    @Override
    public RuleValue visitAndExpr(RuleParser.AndExprContext ctx) {
        if (!visit(ctx.expr(0)).asBoolean()) return RuleValue.FALSE;
        return visit(ctx.expr(1));
    }

    @Override
    public RuleValue visitNotExpr(RuleParser.NotExprContext ctx) {
        return RuleValue.of(!visit(ctx.expr()).asBoolean());
    }

    @Override
    public RuleValue visitGroupExpr(RuleParser.GroupExprContext ctx) {
        return visit(ctx.expr());
    }

    // ── Predicates ────────────────────────────────────────────────────────────

    @Override
    public RuleValue visitComparisonPred(RuleParser.ComparisonPredContext ctx) {
        return RuleValue.of(Predicates.compare(
                visit(ctx.arith(0)), visit(ctx.arith(1)), ctx.compOp().start.getType()));
    }

    @Override
    public RuleValue visitContainsPred(RuleParser.ContainsPredContext ctx) {
        return RuleValue.of(Predicates.contains(visit(ctx.arith(0)), visit(ctx.arith(1))));
    }

    @Override
    public RuleValue visitIsNullPred(RuleParser.IsNullPredContext ctx) {
        return RuleValue.of(visit(ctx.arith()).isNull());
    }

    @Override
    public RuleValue visitIsNotNullPred(RuleParser.IsNotNullPredContext ctx) {
        return RuleValue.of(!visit(ctx.arith()).isNull());
    }

    @Override
    public RuleValue visitIsNumericPred(RuleParser.IsNumericPredContext ctx) {
        return RuleValue.of(visit(ctx.arith()).isNumeric());
    }

    @Override
    public RuleValue visitInPred(RuleParser.InPredContext ctx) {
        RuleValue target = visit(ctx.arith());
        List<RuleValue> items = ctx.inList().arith().stream().map(this::visit).toList();
        return RuleValue.of(Predicates.in(target, items));
    }

    @Override
    public RuleValue visitNotInPred(RuleParser.NotInPredContext ctx) {
        RuleValue target = visit(ctx.arith());
        List<RuleValue> items = ctx.inList().arith().stream().map(this::visit).toList();
        return RuleValue.of(!Predicates.in(target, items));
    }

    @Override
    public RuleValue visitFunctionPred(RuleParser.FunctionPredContext ctx) {
        RuleValue result = visit(ctx.functionCall());
        if (result.getRaw() instanceof Boolean) return result;
        throw new RuleEvaluationException(
                "Function used as predicate must return boolean, got: " + result.getRaw());
    }

    @Override
    public RuleValue visitBoolLiteralPred(RuleParser.BoolLiteralPredContext ctx) {
        return RuleValue.of("true".equalsIgnoreCase(ctx.BOOL_LIT().getText()));
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    @Override
    public RuleValue visitUnaryMinus(RuleParser.UnaryMinusContext ctx) {
        return RuleValue.of(-requireNumeric(visit(ctx.arith()), "unary -"));
    }

    @Override
    public RuleValue visitMulExpr(RuleParser.MulExprContext ctx) {
        int opType = ((TerminalNode) ctx.getChild(1)).getSymbol().getType();
        String opStr = opType == RuleLexer.STAR ? "*" : opType == RuleLexer.SLASH ? "/" : "%";
        double left = requireNumeric(visit(ctx.arith(0)), opStr);
        double right = requireNumeric(visit(ctx.arith(1)), opStr);
        return switch (opType) {
            case RuleLexer.STAR -> RuleValue.of(left * right);
            case RuleLexer.SLASH -> {
                if (right == 0.0) throw new RuleEvaluationException("Division by zero");
                yield RuleValue.of(left / right);
            }
            case RuleLexer.PERCENT -> {
                if (right == 0.0) throw new RuleEvaluationException("Modulo by zero");
                yield RuleValue.of(left % right);
            }
            default -> throw new RuleEvaluationException("Unknown operator: " + opStr);
        };
    }

    @Override
    public RuleValue visitAddExpr(RuleParser.AddExprContext ctx) {
        int opType = ((TerminalNode) ctx.getChild(1)).getSymbol().getType();
        String opStr = opType == RuleLexer.PLUS ? "+" : "-";
        double left = requireNumeric(visit(ctx.arith(0)), opStr);
        double right = requireNumeric(visit(ctx.arith(1)), opStr);
        return switch (opType) {
            case RuleLexer.PLUS -> RuleValue.of(left + right);
            case RuleLexer.MINUS -> RuleValue.of(left - right);
            default -> throw new RuleEvaluationException("Unknown operator: " + opStr);
        };
    }

    @Override
    public RuleValue visitArithParen(RuleParser.ArithParenContext ctx) {
        return visit(ctx.arith());
    }

    @Override
    public RuleValue visitArithFunc(RuleParser.ArithFuncContext ctx) {
        return visit(ctx.functionCall());
    }

    @Override
    public RuleValue visitArithField(RuleParser.ArithFieldContext ctx) {
        return visit(ctx.fieldRef());
    }

    @Override
    public RuleValue visitArithLiteral(RuleParser.ArithLiteralContext ctx) {
        return visit(ctx.literal());
    }

    private double requireNumeric(RuleValue value, String operator) {
        if (!value.isNumeric()) {
            throw new RuleEvaluationException(
                    "Operator '" + operator + "' requires numeric operands, got: " + value.getRaw());
        }
        return value.asDouble();
    }

    // ── Function calls ────────────────────────────────────────────────────────

    @Override
    public RuleValue visitFunctionCall(RuleParser.FunctionCallContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        List<RuleValue> args = ctx.argList() == null
                ? List.of()
                : ctx.argList().arith().stream().map(this::visit).toList();
        log.debug("Calling function '{}' with {} arg(s)", name, args.size());
        return registry.execute(name, args, this.ctx);
    }

    // ── Field references ──────────────────────────────────────────────────────

    @Override
    public RuleValue visitFieldRef(RuleParser.FieldRefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        return this.ctx.resolve(name);
    }

    // ── Literals ──────────────────────────────────────────────────────────────

    @Override
    public RuleValue visitNumberLiteral(RuleParser.NumberLiteralContext ctx) {
        String text = ctx.NUMBER_LIT().getText();
        try {
            return RuleValue.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            throw new RuleEvaluationException("Invalid number literal: " + text);
        }
    }

    @Override
    public RuleValue visitStringLiteral(RuleParser.StringLiteralContext ctx) {
        return RuleValue.of(unquote(ctx.STRING_LIT().getText()));
    }

    private static String unquote(String text) {
        if (text == null || text.length() < 2) return text;
        char quote = text.charAt(0);
        String inner = text.substring(1, text.length() - 1);
        if (quote == '\'') inner = inner.replace("''", "'");
        return inner;
    }

    @Override
    public RuleValue visitBoolLiteral(RuleParser.BoolLiteralContext ctx) {
        return RuleValue.of("true".equalsIgnoreCase(ctx.BOOL_LIT().getText()));
    }

    @Override
    public RuleValue visitNullLiteral(RuleParser.NullLiteralContext ctx) {
        return RuleValue.NULL;
    }
}
