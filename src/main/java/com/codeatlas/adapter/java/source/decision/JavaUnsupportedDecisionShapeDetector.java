package com.codeatlas.adapter.java.source.decision;

import java.util.Optional;

final class JavaUnsupportedDecisionShapeDetector {
    Optional<UnsupportedDecisionShape> detect(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (ifStatement.hasElse() && JavaDecisionSourceSupport.containsDecisionAction(sourceFile, ifStatement)) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_IF_ELSE",
                    "if/else decision shapes are recognized but not supported in Phase 4.2.1"
            ));
        }

        if (!ifStatement.hasBlock() && JavaDecisionSourceSupport.startsWithBodyWord(sourceFile, ifStatement, "throw")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_INLINE_THROW",
                    "Inline conditional throw without a block is recognized but not extracted in Phase 4.2.1"
            ));
        }

        if (!ifStatement.hasBlock() && JavaDecisionSourceSupport.startsWithBodyWord(sourceFile, ifStatement, "return")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_INLINE_RETURN",
                    "Inline conditional return without a block is recognized but not extracted in Phase 4.2.1"
            ));
        }

        if (ifStatement.hasBlock() && JavaDecisionSourceSupport.containsNestedIf(sourceFile, ifStatement)) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_NESTED_IF",
                    "Nested if decision shapes are recognized but not supported in Phase 4.2.1"
            ));
        }

        if (ifStatement.hasBlock() && JavaDecisionSourceSupport.containsBodyWord(sourceFile, ifStatement, "switch")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_SWITCH",
                    "Switch decision shapes are recognized but not supported in Phase 4.2.1"
            ));
        }

        if (ifStatement.hasBlock()
                && (JavaDecisionSourceSupport.containsBodyWord(sourceFile, ifStatement, "try")
                || JavaDecisionSourceSupport.containsBodyWord(sourceFile, ifStatement, "catch"))) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_TRY_CATCH",
                    "try/catch decision shapes are recognized but not supported in Phase 4.2.1"
            ));
        }

        Optional<JavaDecisionSourceSupport.ReturnStatement> directReturn = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                ifStatement.bodyStart(),
                ifStatement.bodyEnd()
        );
        if (directReturn.isPresent()
                && !JavaDecisionSourceSupport.isSimpleReturnExpression(directReturn.get().expression())) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_COMPLEX_RETURN",
                    "Conditional return expression is recognized but too complex for Phase 4.2.1 extraction"
            ));
        }

        if (JavaDecisionSourceSupport.containsTopLevelWord(sourceFile, ifStatement, "return")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_IF_RETURN",
                    "Conditional return shape is recognized but not a simple direct early return"
            ));
        }

        if (JavaDecisionSourceSupport.startsWithBodyWord(sourceFile, ifStatement, "throw")
                && JavaDecisionSourceSupport.containsTopLevelWord(sourceFile, ifStatement, "throw")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_THROW_EXPRESSION",
                    "Conditional throw expression is recognized but not a direct supported throw"
            ));
        }

        if (JavaDecisionSourceSupport.containsTopLevelWord(sourceFile, ifStatement, "throw")) {
            return Optional.of(shape(
                    ifStatement,
                    "UNSUPPORTED_THROW_WITH_ADDITIONAL_STATEMENTS",
                    "Conditional throw with additional statements is recognized but not supported in Phase 4.2.1"
            ));
        }

        return Optional.empty();
    }

    private static UnsupportedDecisionShape shape(
            JavaDecisionSourceSupport.IfStatement ifStatement,
            String kind,
            String message
    ) {
        return new UnsupportedDecisionShape(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                kind,
                ifStatement.condition(),
                ifStatement.snippet(),
                message
        );
    }

    record UnsupportedDecisionShape(
            int ifStart,
            int statementEnd,
            String kind,
            String condition,
            String snippet,
            String message
    ) {
    }
}
