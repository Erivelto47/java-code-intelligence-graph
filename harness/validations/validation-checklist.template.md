# Validation Checklist Template

## Primary blueprint path

## Blueprint approval status

## Derived artifact status

Derived from:

- primary blueprint;
- harness validation checklist template;
- project conventions and existing validations.

## Scope guard

Confirm this checklist validates the blueprint scope and does not expand it.
If this checklist conflicts with the primary blueprint, pause execution and
correct the checklist before continuing.

## git status

Command:

```bash
git status
```

Result:

## branch check

Command:

```bash
git branch --show-current
```

Result:

## compile/test command

Command:

```bash
./gradlew test
```

Result:

## build command

Command:

```bash
./gradlew build
```

Result:

## relevant CLI command

Command:

```bash
# Fill with the phase-specific CLI command.
```

Result:

## fixture comparison

Command:

```bash
# Fill with the fixture comparison command, if applicable.
```

Result:

## git diff --check

Command:

```bash
git diff --check
```

Result:

## report generated

Path:

## no unintended files

Confirmation:

## out-of-scope confirmation

Confirm the validation checked that out-of-scope items from the blueprint were
not implemented.

## no master merge

Confirmation:

## no push unless requested

Confirmation:
