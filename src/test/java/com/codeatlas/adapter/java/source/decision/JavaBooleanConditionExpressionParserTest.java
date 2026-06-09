package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.core.decision.DecisionConditionExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaBooleanConditionExpressionParserTest {
    private final JavaBooleanConditionExpressionParser parser = new JavaBooleanConditionExpressionParser();

    @Test
    void preservesOperatorPrecedence() {
        DecisionConditionExpression expression = parser
                .parseComposed("request.active() || request.admin() && !request.locked()")
                .orElseThrow();

        assertEquals("OR", expression.kind());
        assertEquals("request.active() || request.admin() && !request.locked()", expression.text());
        assertEquals(2, expression.operands().size());
        assertEquals("METHOD_CALL", expression.operands().get(0).kind());
        assertEquals("AND", expression.operands().get(1).kind());
        assertEquals("NOT", expression.operands().get(1).operands().get(1).kind());
        assertEquals("request.locked()", expression.operands().get(1).operands().get(1).operand().text());
    }

    @Test
    void preservesParenthesesAsGroupingNodes() {
        DecisionConditionExpression expression = parser
                .parseComposed("request.active() && (!request.locked() || request.admin())")
                .orElseThrow();

        assertEquals("AND", expression.kind());
        assertEquals("METHOD_CALL", expression.operands().get(0).kind());
        assertEquals("GROUP", expression.operands().get(1).kind());
        assertEquals("(!request.locked() || request.admin())", expression.operands().get(1).text());
        assertEquals("OR", expression.operands().get(1).operand().kind());
    }

    @Test
    void capturesComparisonLeavesAndMethodPredicates() {
        DecisionConditionExpression expression = parser
                .parseComposed("request.age() >= 18 && request.name() != null && request.email().contains(\"@\")")
                .orElseThrow();

        assertEquals("AND", expression.kind());
        assertEquals("COMPARISON", expression.operands().get(0).kind());
        assertEquals(">=", expression.operands().get(0).operator());
        assertEquals("request.age()", expression.operands().get(0).left());
        assertEquals("18", expression.operands().get(0).right());
        assertEquals("COMPARISON", expression.operands().get(1).kind());
        assertEquals("!=", expression.operands().get(1).operator());
        assertEquals("METHOD_CALL", expression.operands().get(2).kind());
        assertEquals("request.email().contains(\"@\")", expression.operands().get(2).text());
    }

    @Test
    void leavesSimplePredicateBackwardCompatible() {
        assertTrue(parser.parseComposed("request.active()").isEmpty());
    }
}
