package com.codeatlas.adapter.java.source.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JavaElseIfDecisionExtractor {
    Optional<ElseIfDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (!ifStatement.hasElse()
                || !JavaDecisionSourceSupport.startsWithWord(
                sourceFile.maskedSource(),
                ifStatement.elseBodyStart(),
                "if")) {
            return Optional.empty();
        }

        List<ChainBranch> branches = new ArrayList<>();
        branches.add(new ChainBranch(
                "IF",
                ifStatement.condition(),
                1,
                ifStatement.bodyStart(),
                ifStatement.bodyEnd()
        ));

        JavaDecisionSourceSupport.IfStatement current = ifStatement;
        int order = 2;
        while (current.hasElse()
                && JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), current.elseBodyStart(), "if")) {
            Optional<JavaDecisionSourceSupport.IfStatement> elseIf = JavaDecisionSourceSupport.parseIfStatement(
                    sourceFile,
                    new JavaDecisionSourceSupport.MethodRange(0, current.elseBodyStart(), ifStatement.statementEnd()),
                    current.elseBodyStart()
            );
            if (elseIf.isEmpty()) {
                break;
            }

            current = elseIf.get();
            branches.add(new ChainBranch(
                    "ELSE_IF",
                    current.condition(),
                    order,
                    current.bodyStart(),
                    current.bodyEnd()
            ));
            order++;
        }

        if (current.hasElse()
                && !JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), current.elseBodyStart(), "if")) {
            branches.add(new ChainBranch(
                    "ELSE",
                    null,
                    order,
                    current.elseBodyStart(),
                    current.elseBodyEnd()
            ));
        }

        if (branches.stream().noneMatch(branch -> "ELSE_IF".equals(branch.kind()))) {
            return Optional.empty();
        }

        return Optional.of(new ElseIfDecision(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                ifStatement.condition(),
                ifStatement.snippet(),
                List.copyOf(branches)
        ));
    }

    record ElseIfDecision(
            int ifStart,
            int statementEnd,
            String condition,
            String snippet,
            List<ChainBranch> branches
    ) {
    }

    record ChainBranch(
            String kind,
            String condition,
            int order,
            int bodyStart,
            int bodyEnd
    ) {
    }
}
