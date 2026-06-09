# Phase 4 Decision Trace Closeout

## Recommendation

Phase 4 is ready to close as a deterministic, method-local Java Decision Trace
MVP after human review of the closeout report. The MVP covers common Java
decision structures and intentionally does not claim full Java semantic or data
flow coverage.

## Closed MVP Scope

Phase 4 emits source-grounded facts only. It does not perform AI
interpretation, symbolic execution, runtime tracing, or broad data-flow
analysis. The supported command remains method-entrypoint oriented:

```bash
./gradlew run --args="analyze-decisions --project <project> --entrypoint <fully.qualified.Type.method>"
```

`decisions.json` is the primary artifact. `decisions.md` and `decisions.mmd`
are derived views and must not introduce facts absent from JSON.

Decision Trace artifacts remain separate from Flow Graph, Project Index, and
entrypoint artifacts. They may link to those artifacts, but must not duplicate
node lists, edge lists, boundaries, type inventories, or entrypoint inventories.

## Fixture Matrix

| Construct | Supported | Artifact Shape | Fixture | Test | Notes |
| --- | --- | --- | --- | --- | --- |
| Direct block if/throw | Yes | `CONDITIONAL_THROW` with `THROW` and `CONTINUE` outcomes | `01-if-throw-validation` | `DecisionTraceFixtureContractTest.ifThrowValidationFixtureMatchesGeneratedArtifactsExactly` | Phase 4.1 production MVP shape. |
| Simple validation if | Yes | `IF_CONDITION` with validation outcomes | `01-simple-validation` | `DecisionTraceFixtureContractTest.phaseFourDecisionFixturesContainValidDecisionJson` | Contract seed fixture, not an exact generated-artifact regression. |
| Direct if/return early return | Yes | `EARLY_RETURN` with `RETURN` and `CONTINUE` outcomes | `02-if-return-early-return` | `DecisionTraceFixtureContractTest.earlyReturnFixtureMatchesGeneratedArtifactsExactly` | Phase 4.2 return support. |
| Early return contract seed | Yes | `EARLY_RETURN` with `RETURN` and `CONTINUE` outcomes | `02-early-return` | `DecisionTraceFixtureContractTest.phaseFourDecisionFixturesContainValidDecisionJson` | Contract seed fixture. |
| Repository-backed conditional throw | Yes | `CONDITIONAL_THROW` with deterministic source evidence | `03-conditional-throw` | `DecisionTraceFixtureContractTest.phaseFourDecisionFixturesContainValidDecisionJson` | Contract seed fixture. |
| Unsupported decision shapes | Partial | `unresolved[]` records with source expression and reason | `03-unresolved-decision-shapes` | `DecisionTraceFixtureContractTest.unresolvedDecisionShapesFixtureMatchesGeneratedArtifactsExactly` | Unsupported recognized shapes are not guessed. |
| If/throw with pre-statements | Yes | `CONDITIONAL_THROW`; pre-statements remain source evidence, not separate facts | `04-if-throw-with-pre-statements` | `DecisionTraceFixtureContractTest.ifThrowWithPreStatementsFixtureMatchesGeneratedArtifactsExactly` | Only simple pre-statements before final direct throw are covered. |
| Single-line if/throw | Yes | `CONDITIONAL_THROW` with inline source evidence | `05-single-line-if-throw` | `DecisionTraceFixtureContractTest.singleLineIfThrowFixtureMatchesGeneratedArtifactsExactly` | Direct inline literal throw only. |
| If/else return branches | Yes | `IF_ELSE_CONDITION` with explicit branch outcomes | `06-if-else-return-branches` | `DecisionTraceFixtureContractTest.ifElseReturnBranchesFixtureMatchesGeneratedArtifactsExactly` | Narrow direct branch extraction. |
| If/else mixed throw/return | Yes | `IF_ELSE_CONDITION` with `THROW` and `RETURN` outcomes | `07-if-else-throw-return-branches` | `DecisionTraceFixtureContractTest.ifElseThrowReturnBranchesFixtureMatchesGeneratedArtifactsExactly` | Narrow direct branch extraction. |
| Method-local helper decision call | Yes | Caller decision references one-level same-class helper evidence | `08-method-local-decision-call` | `DecisionTraceFixtureContractTest.methodLocalDecisionCallFixtureMatchesGeneratedArtifactsExactly` | Method-local only; no interprocedural expansion beyond implemented references. |
| Else-if chain | Yes | `IF_ELSE_IF_CHAIN` with ordered branch outcomes | `09-else-if-chain` | `DecisionTraceFixtureContractTest.elseIfChainFixtureMatchesGeneratedArtifactsExactly` | Direct chain only. |
| Nested if | Yes | Multiple method-local decision records preserving nesting evidence | `10-nested-if-decision` | `DecisionTraceFixtureContractTest.nestedIfDecisionFixtureMatchesGeneratedArtifactsExactly` | Does not imply full path-condition analysis. |
| Boolean `&&`, `||`, `!`, grouping | Yes | `expression.conditionExpression` tree with deterministic leaf nodes | `11-boolean-and-or-not-condition` | `DecisionTraceFixtureContractTest.booleanAndOrNotConditionFixtureMatchesGeneratedArtifactsExactly` | Captures expression structure, not runtime truth tables. |
| Comparison and method predicates | Yes | Condition expression leaves for comparisons and method calls | `12-comparison-and-method-predicate-condition` | `DecisionTraceFixtureContractTest.comparisonAndMethodPredicateConditionFixtureMatchesGeneratedArtifactsExactly` | Source-grounded predicate text only. |
| Return ternary | Yes | `TERNARY_CONDITION` with return outcomes | `13-return-ternary-decision` | `DecisionTraceFixtureContractTest.returnTernaryDecisionFixtureMatchesGeneratedArtifactsExactly` | Direct expression shape. |
| Assignment ternary | Yes | `TERNARY_CONDITION` with `ASSIGN` outcomes | `14-assignment-ternary-decision` | `DecisionTraceFixtureContractTest.assignmentTernaryDecisionFixtureMatchesGeneratedArtifactsExactly` | Direct local assignment shape. |
| Nested ternary | Yes | Nested `TERNARY_CONDITION` records | `15-nested-ternary-decision` | `DecisionTraceFixtureContractTest.nestedTernaryDecisionFixtureMatchesGeneratedArtifactsExactly` | Deterministic syntax nesting, not broad expression evaluation. |
| Switch statement | Yes | `SWITCH_DECISION` with case/default outcomes | `15-switch-statement-decision` | `DecisionTraceFixtureContractTest.switchStatementDecisionFixtureMatchesGeneratedArtifactsExactly` | Classic visible cases. |
| Switch expression | Yes | `SWITCH_EXPRESSION_DECISION` with assignment outcomes | `16-switch-expression-decision` | `DecisionTraceFixtureContractTest.switchExpressionDecisionFixtureMatchesGeneratedArtifactsExactly` | Direct expression assignment shape. |
| Switch fall-through | Yes | `SWITCH_DECISION` preserving syntactically visible fall-through | `17-switch-fallthrough-decision` | `DecisionTraceFixtureContractTest.switchFallthroughDecisionFixtureMatchesGeneratedArtifactsExactly` | No runtime reachability analysis beyond source shape. |
| Optional `orElseThrow` | Yes | `OPTIONAL_BRANCH` with present/fallback outcomes | `18-optional-or-else-throw-decision` | `DecisionTraceFixtureContractTest.optionalOrElseThrowDecisionFixtureMatchesGeneratedArtifactsExactly` | Supported idiom only. |
| Optional `orElse` fallback | Yes | `OPTIONAL_BRANCH` with fallback assignment | `19-optional-or-else-fallback-decision` | `DecisionTraceFixtureContractTest.optionalFallbackDecisionFixtureMatchesGeneratedArtifactsExactly` | Supported idiom only. |
| Optional `ifPresentOrElse` | Yes | `OPTIONAL_BRANCH` with call outcomes | `20-optional-if-present-or-else-decision` | `DecisionTraceFixtureContractTest.optionalIfPresentOrElseDecisionFixtureMatchesGeneratedArtifactsExactly` | Supported idiom only. |
| Stream `filter` | Yes | `STREAM_FILTER_DECISION` with `FILTER` outcome | `21-stream-filter-decision` | `DecisionTraceFixtureContractTest.streamFilterDecisionFixtureMatchesGeneratedArtifactsExactly` | Predicate source is captured deterministically. |
| Stream `anyMatch` | Yes | `STREAM_MATCH_DECISION` and related local early return where present | `22-stream-any-match-decision` | `DecisionTraceFixtureContractTest.streamAnyMatchDecisionFixtureMatchesGeneratedArtifactsExactly` | Supported terminal match idiom. |
| Stream `allMatch` / `noneMatch` | Yes | `STREAM_MATCH_DECISION` with assignment outcomes | `23-stream-all-none-match-decision` | `DecisionTraceFixtureContractTest.streamAllNoneMatchDecisionFixtureMatchesGeneratedArtifactsExactly` | Supported terminal match idioms. |
| Endpoint-based decision analysis | No | Not emitted by Phase 4 MVP | None | Not applicable | Future work. |
| Project-wide decision index | No | `decisions-index.*` remains future work | None | Not applicable | Future work. |
| Cross-method/interprocedural decision expansion | No | Unsupported beyond implemented method-local helper references | None | Not applicable | Keep method-local MVP boundary. |
| Broad Java data flow or symbolic execution | No | Not emitted | None | Not applicable | Intentional boundary. |
| AI interpretation or business synthesis | No | Not emitted | None | Not applicable | Reserved for Phase 5. |
| IntelliJ PSI adapter coupling | No | Not required by Phase 4 MVP docs | None | Not applicable | Future adapter boundaries may be documented later. |
| Unrecognized unsupported constructs | Partial | Ignored, unresolved, or preserved as text according to extractor support | `03-unresolved-decision-shapes` where recognized | `DecisionTraceFixtureContractTest.unresolvedDecisionShapesFixtureMatchesGeneratedArtifactsExactly` | Do not invent facts for unknown shapes. |

## Limitations

- Phase 4 is method-local and source-text oriented.
- Supported Optional and Stream coverage is idiom-based, not complete Java API
  semantics.
- Branch outcomes are explicit source-level outcomes, not runtime path proofs.
- Condition expression trace preserves deterministic syntax structure; it does
  not evaluate values.
- Unsupported constructs may be ignored, represented as unresolved records, or
  captured as source text according to documented extractor behavior.
- Limitations are intentional and versioned as part of the Phase 4 contract.

## Validation

The closeout validation should run:

```bash
git status
git branch --show-current
./gradlew clean test
./gradlew test
./gradlew build
./gradlew test --tests com.codeatlas.examples.DecisionTraceFixtureContractTest
git diff --check
```

A representative CLI regression can also generate artifacts for one fixture and
compare them with `expected/` using `diff -u`. Full fixture coverage is provided
by `DecisionTraceFixtureContractTest`, which invokes `analyze-decisions` for
each production Phase 4 fixture and compares JSON, Markdown, and Mermaid outputs
exactly.

## Phase 5 Handoff

Recommended next branch:

```text
phase-5-ai-interpretation-layer
```

Phase 5 should consume Project Index, Flow Graph, and Decision Trace artifacts
as deterministic inputs. It should not change the Phase 4 artifact contract
unless a new versioned contract phase explicitly does so.
