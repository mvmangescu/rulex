package com.rulex.engine;

import com.rulex.grammar.RuleBaseVisitor;
import com.rulex.grammar.RuleParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.function.Supplier;

public class ExplainingEvaluator extends RuleBaseVisitor<TraceNode> {

    private final RuleEvaluator eval;
    private final String expression;

    public ExplainingEvaluator(RuleEvaluator eval, String expression) {
        this.eval = eval;
        this.expression = expression;
    }

    @Override
    public TraceNode visitProgram(RuleParser.ProgramContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public TraceNode visitOrExpr(RuleParser.OrExprContext ctx) {
        TraceNode left = child(ctx.expr(0));
        TraceNode right = child(ctx.expr(1));
        return TraceNode.compound(src(ctx), "OR", left.result() || right.result(), List.of(left, right));
    }

    @Override
    public TraceNode visitAndExpr(RuleParser.AndExprContext ctx) {
        TraceNode left = child(ctx.expr(0));
        TraceNode right = child(ctx.expr(1));
        return TraceNode.compound(src(ctx), "AND", left.result() && right.result(), List.of(left, right));
    }

    @Override
    public TraceNode visitNotExpr(RuleParser.NotExprContext ctx) {
        TraceNode child = child(ctx.expr());
        return TraceNode.compound(src(ctx), "NOT", !child.result(), List.of(child));
    }

    @Override
    public TraceNode visitGroupExpr(RuleParser.GroupExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public TraceNode visitComparisonPred(RuleParser.ComparisonPredContext ctx) {
        return leaf(ctx, "COMPARISON", () -> {
            RuleValue left = eval.visit(ctx.arith(0));
            RuleValue right = eval.visit(ctx.arith(1));
            boolean result = Predicates.compare(left, right, ctx.compOp().start.getType());
            return TraceNode.leaf(src(ctx), "COMPARISON", result,
                    left.asString() + " " + ctx.compOp().getText() + " " + right.asString());
        });
    }

    @Override
    public TraceNode visitContainsPred(RuleParser.ContainsPredContext ctx) {
        return leaf(ctx, "CONTAINS", () -> {
            RuleValue left = eval.visit(ctx.arith(0));
            RuleValue right = eval.visit(ctx.arith(1));
            return TraceNode.leaf(src(ctx), "CONTAINS", Predicates.contains(left, right),
                    left.asString() + " contains " + right.asString());
        });
    }

    @Override
    public TraceNode visitNotContainsPred(RuleParser.NotContainsPredContext ctx) {
        return leaf(ctx, "NOT_CONTAINS", () -> {
            RuleValue left = eval.visit(ctx.arith(0));
            RuleValue right = eval.visit(ctx.arith(1));
            return TraceNode.leaf(src(ctx), "NOT_CONTAINS", !Predicates.contains(left, right),
                    left.asString() + " not contains " + right.asString());
        });
    }

    @Override
    public TraceNode visitIsNullPred(RuleParser.IsNullPredContext ctx) {
        return leaf(ctx, "IS_NULL", () -> {
            RuleValue val = eval.visit(ctx.arith());
            return TraceNode.leaf(src(ctx), "IS_NULL", val.isNull(), val.asString() + " is null");
        });
    }

    @Override
    public TraceNode visitIsNotNullPred(RuleParser.IsNotNullPredContext ctx) {
        return leaf(ctx, "IS_NOT_NULL", () -> {
            RuleValue val = eval.visit(ctx.arith());
            return TraceNode.leaf(src(ctx), "IS_NOT_NULL", !val.isNull(), val.asString() + " is not null");
        });
    }

    @Override
    public TraceNode visitIsNumericPred(RuleParser.IsNumericPredContext ctx) {
        return leaf(ctx, "IS_NUMERIC", () -> {
            RuleValue val = eval.visit(ctx.arith());
            return TraceNode.leaf(src(ctx), "IS_NUMERIC", val.isNumeric(), val.asString() + " is numeric");
        });
    }

    @Override
    public TraceNode visitInPred(RuleParser.InPredContext ctx) {
        return leaf(ctx, "IN", () -> {
            RuleValue target = eval.visit(ctx.arith());
            List<RuleValue> items = ctx.inList().arith().stream().map(eval::visit).toList();
            return TraceNode.leaf(src(ctx), "IN", Predicates.in(target, items),
                    target.asString() + " in list");
        });
    }

    @Override
    public TraceNode visitNotInPred(RuleParser.NotInPredContext ctx) {
        return leaf(ctx, "NOT_IN", () -> {
            RuleValue target = eval.visit(ctx.arith());
            List<RuleValue> items = ctx.inList().arith().stream().map(eval::visit).toList();
            return TraceNode.leaf(src(ctx), "NOT_IN", !Predicates.in(target, items),
                    target.asString() + " not in list");
        });
    }

    @Override
    public TraceNode visitFunctionPred(RuleParser.FunctionPredContext ctx) {
        return leaf(ctx, "FUNCTION", () -> {
            String funcName = ctx.functionCall().IDENTIFIER().getText();
            boolean result = eval.visit(ctx.functionCall()).asBoolean();
            return TraceNode.leaf(src(ctx), "FUNCTION", result, funcName + "() = " + result);
        });
    }

    @Override
    public TraceNode visitBoolLiteralPred(RuleParser.BoolLiteralPredContext ctx) {
        boolean result = "true".equalsIgnoreCase(ctx.BOOL_LIT().getText());
        return TraceNode.leaf(src(ctx), "BOOL_LITERAL", result, ctx.BOOL_LIT().getText());
    }

    private String src(ParserRuleContext ctx) {
        if (ctx.start == null || ctx.stop == null) return "";
        return expression.substring(ctx.start.getStartIndex(), ctx.stop.getStopIndex() + 1);
    }

    private TraceNode child(ParserRuleContext ctx) {
        try {
            return visit(ctx);
        } catch (Exception e) {
            return TraceNode.error(src(ctx), "UNKNOWN", e.getMessage());
        }
    }

    private TraceNode leaf(ParserRuleContext ctx, String type, Supplier<TraceNode> compute) {
        try {
            return compute.get();
        } catch (Exception e) {
            return TraceNode.error(src(ctx), type, e.getMessage());
        }
    }
}
