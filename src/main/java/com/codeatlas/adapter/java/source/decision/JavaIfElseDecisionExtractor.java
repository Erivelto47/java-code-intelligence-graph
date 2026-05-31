package com.codeatlas.adapter.java.source.decision;

import java.util.Optional;

final class JavaIfElseDecisionExtractor {
    enum BranchAction {
        RETURN,
        THROW
    }

    Optional<IfElseDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (!ifStatement.hasBlock() || !ifStatement.hasElse() || !ifStatement.elseHasBlock()) {
            return Optional.empty();
        }

        Optional<BranchOutcome> trueOutcome = parseBranchOutcome(
                sourceFile,
                ifStatement.bodyStart(),
                ifStatement.bodyEnd()
        );
        Optional<BranchOutcome> falseOutcome = parseBranchOutcome(
                sourceFile,
                ifStatement.elseBodyStart(),
                ifStatement.elseBodyEnd()
        );
        if (trueOutcome.isEmpty() || falseOutcome.isEmpty()) {
            return Optional.empty();
        }

        if (!isSupportedBranchPair(trueOutcome.get(), falseOutcome.get())) {
            return Optional.empty();
        }

        return Optional.of(new IfElseDecision(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                ifStatement.condition(),
                ifStatement.snippet(),
                trueOutcome.get(),
                falseOutcome.get()
        ));
    }

    private static Optional<BranchOutcome> parseBranchOutcome(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int bodyStart,
            int bodyEnd
    ) {
        Optional<JavaDecisionSourceSupport.ReturnStatement> returnStatement = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                bodyStart,
                bodyEnd
        );
        if (returnStatement.isPresent()) {
            String expression = returnStatement.get().expression();
            if (!JavaDecisionSourceSupport.isSimpleReturnExpression(expression)) {
                return Optional.empty();
            }
            return Optional.of(new BranchOutcome(
                    BranchAction.RETURN,
                    expression,
                    JavaDecisionSourceSupport.returnTarget(expression),
                    null,
                    null
            ));
        }

        Optional<JavaDecisionSourceSupport.ThrowStatement> throwStatement = JavaDecisionSourceSupport.parseDirectThrow(
                sourceFile,
                bodyStart,
                bodyEnd
        ).filter(JavaDecisionSourceSupport.ThrowStatement::hasDirectLiteralMessage);
        return throwStatement.map(statement -> new BranchOutcome(
                BranchAction.THROW,
                null,
                statement.exceptionType(),
                statement.exceptionType(),
                statement.message()
        ));
    }

    private static boolean isSupportedBranchPair(BranchOutcome trueOutcome, BranchOutcome falseOutcome) {
        if (trueOutcome.action() == BranchAction.RETURN && falseOutcome.action() == BranchAction.RETURN) {
            return true;
        }
        return trueOutcome.action() != falseOutcome.action()
                && (trueOutcome.action() == BranchAction.RETURN || trueOutcome.action() == BranchAction.THROW)
                && (falseOutcome.action() == BranchAction.RETURN || falseOutcome.action() == BranchAction.THROW);
    }

    record IfElseDecision(
            int ifStart,
            int statementEnd,
            String condition,
            String snippet,
            BranchOutcome trueOutcome,
            BranchOutcome falseOutcome
    ) {
    }

    record BranchOutcome(
            BranchAction action,
            String returnExpression,
            String target,
            String exceptionType,
            String message
    ) {
    }
}
