# Phase 4 Decision Trace Fixtures

This folder contains small, versioned, controlled fixtures for the Phase 4
Decision Trace contract.

## Objective

The examples define expected deterministic decision artifacts before the
production decision parser exists. They are meant to be readable by humans,
useful for AI-assisted debugging, and stable enough to become regression tests.

## Relationship With Phase 4

- `expected/decisions.json` is the primary contract for each fixture.
- `expected/decisions.md` and `expected/decisions.mmd` are derived views.
- Decision Trace must remain separate from Flow Graph and Project Index.
- Fixtures may reference flow or project index artifacts, but they must not
  duplicate them.
- No AI interpretation is expected or encoded in these fixtures.

## Structure

Each fixture should focus on one decision shape:

- `src/main/java/...` contains the minimal Java source.
- `README.md` explains the scenario, scope, expected decision, and acceptance
  criteria.
- `expected/decisions.json` contains the contract artifact.
- `expected/decisions.md` contains the human-readable derived view.
- `expected/decisions.mmd` contains the Mermaid derived view when useful.

Complex cases should be added as isolated fixtures. Do not combine unrelated
decision shapes in the same example.

## Current Fixtures

| Example | Scope | Validates |
| --- | --- | --- |
| `01-if-throw-validation` | `com.example.UserService.create` | Phase 4.1 extractor MVP for `if (...) { throw new SomeException("message"); }`. |
| `01-simple-validation` | `com.example.decisiontrace.simplevalidation.UserRegistrationService.register` | Null or blank name validation with `THROW` and `CONTINUE` outcomes. |
| `02-if-return-early-return` | `com.example.decisiontrace.ifreturn.ImportService.process` | Phase 4.2 simple direct `if (...) { return ...; }` extraction with `RETURN` outcome. |
| `02-early-return` | `com.example.decisiontrace.earlyreturn.ImportService.process` | Early return when an input is already processed. |
| `03-unresolved-decision-shapes` | `com.example.decisiontrace.unresolved.RegistrationGuard.validate` | Phase 4.2 recognized unsupported Java decision shapes emitted as unresolved records. |
| `03-conditional-throw` | `com.example.decisiontrace.conditionalthrow.EmailRegistrationService.register` | Repository-backed e-mail uniqueness check that throws when true. |

## Future Fixtures

Planned examples:

- `04-if-else-business-rule`
- `05-unsupported-optional-chain`

Unsupported constructs should preserve source evidence and produce
decision-specific unresolved items instead of guessed decisions.
