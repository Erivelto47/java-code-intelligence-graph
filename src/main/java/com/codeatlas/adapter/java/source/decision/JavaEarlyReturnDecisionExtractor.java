package com.codeatlas.adapter.java.source.decision;

import java.util.Optional;

public final class JavaEarlyReturnDecisionExtractor {
    Optional<EarlyReturnDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (!ifStatement.hasBlock()
                || ifStatement.hasElse()
                || JavaDecisionSourceSupport.containsNestedIf(sourceFile, ifStatement)) {
            return Optional.empty();
        }

        Optional<JavaDecisionSourceSupport.ReturnStatement> returnStatement = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                ifStatement.bodyStart(),
                ifStatement.bodyEnd()
        );
        if (returnStatement.isEmpty()) {
            return Optional.empty();
        }

        String expression = returnStatement.get().expression();
        if (!JavaDecisionSourceSupport.isSimpleReturnExpression(expression)) {
            return Optional.empty();
        }

        return Optional.of(new EarlyReturnDecision(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                ifStatement.condition(),
                ifStatement.snippet(),
                expression,
                JavaDecisionSourceSupport.returnTarget(expression)
        ));
    }

    record EarlyReturnDecision(
            int ifStart,
            int blockEnd,
            String condition,
            String snippet,
            String returnExpression,
            String returnTarget
    ) {
    }
}
