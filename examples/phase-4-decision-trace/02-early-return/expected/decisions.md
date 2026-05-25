# Decision Trace

Scope: `com.example.decisiontrace.earlyreturn.ImportService.process`

Source:

- Flow: not available
- Project Index: not available

## Decisions

| ID | Kind | Category | Method | Location | Expression | Outcomes |
| --- | --- | --- | --- | --- | --- | --- |
| `decision:02-early-return:process:already-processed` | EARLY_RETURN | BUSINESS_RULE | `com.example.decisiontrace.earlyreturn.ImportService.process` | `src/main/java/com/example/decisiontrace/earlyreturn/ImportService.java:5` | `request.processed()` | true -> RETURN `ProcessingResult.alreadyProcessed`; false -> CONTINUE |

## Unresolved

No unresolved decision items.
