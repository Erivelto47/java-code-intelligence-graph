package com.codeatlas.adapter.java.source.decision;

import java.util.Optional;

final class JavaIfElseDecisionExtractor {
    Optional<IfElseDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (!ifStatement.hasBlock() || !ifStatement.hasElse() || !ifStatement.elseHasBlock()) {
            return Optional.empty();
        }

        Optional<JavaDecisionSourceSupport.ReturnStatement> trueReturn = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                ifStatement.bodyStart(),
                ifStatement.bodyEnd()
        );
        Optional<JavaDecisionSourceSupport.ReturnStatement> falseReturn = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                ifStatement.elseBodyStart(),
                ifStatement.elseBodyEnd()
        );
        if (trueReturn.isEmpty() || falseReturn.isEmpty()) {
            return Optional.empty();
        }

        String trueExpression = trueReturn.get().expression();
        String falseExpression = falseReturn.get().expression();
        if (!JavaDecisionSourceSupport.isSimpleReturnExpression(trueExpression)
                || !JavaDecisionSourceSupport.isSimpleReturnExpression(falseExpression)) {
            return Optional.empty();
        }

        return Optional.of(new IfElseDecision(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                ifStatement.condition(),
                ifStatement.snippet(),
                trueExpression,
                JavaDecisionSourceSupport.returnTarget(trueExpression),
                falseExpression,
                JavaDecisionSourceSupport.returnTarget(falseExpression)
        ));
    }

    record IfElseDecision(
            int ifStart,
            int statementEnd,
            String condition,
            String snippet,
            String trueReturnExpression,
            String trueReturnTarget,
            String falseReturnExpression,
            String falseReturnTarget
    ) {
    }
}
