package com.rulex.engine;

import com.rulex.engine.function.FunctionRegistry;
import com.rulex.grammar.RuleParser;
import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ExplainingEvaluator extends RuleEvaluator {

    private final String expression;
    private final Deque<List<TraceNode>> stack = new ArrayDeque<>();
    @Getter
    private TraceNode trace;

    public ExplainingEvaluator(EvaluationContext ctx, FunctionRegistry registry, int maxSteps, String expression) {
        super(ctx, registry, maxSteps);
        this.expression = expression;
    }

    @Override
    public RuleValue visit(ParseTree tree) {
        if (!(tree instanceof RuleParser.ExprContext exprCtx)
                || tree instanceof RuleParser.GroupExprContext) {
            return super.visit(tree);
        }
        String src = src(exprCtx);
        String type = type(exprCtx);
        stack.push(new ArrayList<>());
        RuleValue value;
        try {
            value = super.visit(tree);
        } catch (Exception e) {
            stack.pop();
            addNode(TraceNode.error(src, type, e.getMessage()));
            throw e;
        }
        List<TraceNode> children = stack.pop();
        addNode(children.isEmpty()
                ? TraceNode.leaf(src, type, value.asBoolean(), src)
                : TraceNode.compound(src, type, value.asBoolean(), children));
        return value;
    }

    @Override
    public RuleValue visitAndExpr(RuleParser.AndExprContext ctx) {
        RuleValue left = visit(ctx.expr(0));
        RuleValue right = visit(ctx.expr(1));
        return RuleValue.of(left.asBoolean() && right.asBoolean());
    }

    @Override
    public RuleValue visitOrExpr(RuleParser.OrExprContext ctx) {
        RuleValue left = visit(ctx.expr(0));
        RuleValue right = visit(ctx.expr(1));
        return RuleValue.of(left.asBoolean() || right.asBoolean());
    }

    private void addNode(TraceNode node) {
        if (stack.isEmpty()) trace = node;
        else stack.peek().add(node);
    }

    private String src(ParserRuleContext ctx) {
        if (ctx.start == null || ctx.stop == null) return "";
        return expression.substring(ctx.start.getStartIndex(), ctx.stop.getStopIndex() + 1);
    }

    private static String type(RuleParser.ExprContext ctx) {
        return switch (ctx.getClass().getSimpleName().replace("Context", "")) {
            case "AndExpr" -> "AND";
            case "OrExpr" -> "OR";
            case "NotExpr" -> "NOT";
            case "ComparisonPred" -> "COMPARISON";
            case "ContainsPred" -> "CONTAINS";
            case "NotContainsPred" -> "NOT_CONTAINS";
            case "IsNullPred" -> "IS_NULL";
            case "IsNotNullPred" -> "IS_NOT_NULL";
            case "IsNumericPred" -> "IS_NUMERIC";
            case "InPred" -> "IN";
            case "NotInPred" -> "NOT_IN";
            case "FunctionPred" -> "FUNCTION";
            case "BoolLiteralPred" -> "BOOL_LITERAL";
            default -> ctx.getClass().getSimpleName().replace("Context", "");
        };
    }
}
