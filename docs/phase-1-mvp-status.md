# Phase 1 MVP Status - Java Flow Graph

## Status

Status: CONSOLIDATED / APPROVED

Phase 1 is consolidated as the initial technical MVP for Java Flow Graph
generation in Code Atlas. It is a deterministic, source-level graph generator
for Java class/method entrypoints. It is not a complete Java static analysis
solution and does not claim full runtime, Spring container, polymorphic, or data
flow semantics.

## Scope

Phase 1 covers:

- Java class/method entrypoint input.
- `flow.json` generation.
- `flow.md` generation.
- `flow.mmd` generation.
- `context-pack.md` generation.
- `agent-handoff.md` generation.
- Receiver field resolution from declared field or constructor-injected type.
- Interface single implementation resolution.
- Multiple interface implementations recorded as unresolved.
- Repository boundaries.
- Framework, external, and HTTP client boundaries.
- Conservative worklist/DFS traversal through resolved internal methods.
- Core model and analyzer contract independent from PSI and adapter packages.

## Primary Contract

The active Phase 1 contract is:

- [docs/phase-1-flow-graph-contract.md](phase-1-flow-graph-contract.md)

`flow.json` is the primary deterministic artifact. Markdown, Mermaid,
context-pack, and agent-handoff files are derived from the same `FlowGraph`
instance. AI may explain or audit the artifacts, but AI output is not the
primary source of graph facts and must not invent nodes, edges, resolutions, or
runtime behavior.

## Capabilities

Approved Phase 1 capabilities:

- Detect the requested class/method entrypoint.
- Resolve direct calls on concrete receivers.
- Resolve field receivers to their declared type.
- Resolve an interface call when exactly one eligible local implementation
  exists.
- Record `RESOLVES_TO` with `evidence=INFERRED` and `confidence=HIGH`.
- Record multiple implementations as `unresolved` with concrete candidates.
- Classify repository calls as boundaries.
- Classify HTTP client, framework, logging, and external symbols as boundaries.
- Continue traversal through resolved internal methods.
- Produce consistent derived artifacts from the deterministic graph.

## Validated Examples

All examples live under `examples/phase-1-java-flow/`. Each example contains
`src/main/java/...`, a local `README.md`, and expected artifacts in
`code-atlas.expected/`.

### 01-direct-method-call

- Path: `examples/phase-1-java-flow/01-direct-method-call`
- Objective: validate a direct call from a controller to a concrete service and
  continued traversal into an internal method.
- Entrypoint: `com.example.direct.OrderController.create`
- Validated behavior: `OrderController.create` calls
  `OrderService.createOrder`, which calls `OrderService.validate`; no unresolved
  symbols are expected.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

### 02-controller-service

- Path: `examples/phase-1-java-flow/02-controller-service`
- Objective: validate controller to concrete service traversal through an
  injected field.
- Entrypoint: `com.example.controllerservice.CustomerController.register`
- Validated behavior: `CustomerController.register` calls
  `CustomerService.register`; the service then calls `normalize` and `persist`;
  no unresolved symbols are expected.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

### 03-interface-single-implementation

- Path: `examples/phase-1-java-flow/03-interface-single-implementation`
- Objective: validate interface dispatch when there is one local concrete
  implementation.
- Entrypoint: `com.example.interfaces.single.RegistrationController.register`
- Validated behavior: `RegistrationUseCase.create` resolves to
  `RegistrationService.create` with inferred high-confidence evidence; traversal
  continues into `validate`; `UserRepository.save` is a repository boundary.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

### 04-interface-multiple-implementations

- Path: `examples/phase-1-java-flow/04-interface-multiple-implementations`
- Objective: validate conservative handling of ambiguous interface dispatch.
- Entrypoint: `com.example.interfaces.multiple.NotificationController.send`
- Validated behavior: `NotificationSender.send` is called factually, but
  `EmailNotificationSender.send` and `SmsNotificationSender.send` remain
  candidates in `unresolved` with reason `MULTIPLE_IMPLEMENTATIONS`.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

### 05-repository-boundary

- Path: `examples/phase-1-java-flow/05-repository-boundary`
- Objective: validate repository interfaces without local implementations as
  deterministic boundaries.
- Entrypoint: `com.example.repository.AccountService.openAccount`
- Validated behavior: `AccountRepository.findByDocument` and
  `AccountRepository.save` are repository boundaries; traversal does not invent
  repository implementations.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

### 06-external-client-boundary

- Path: `examples/phase-1-java-flow/06-external-client-boundary`
- Objective: validate external or HTTP client boundaries after a resolved local
  implementation.
- Entrypoint: `com.example.externalclient.PaymentService.pay`
- Validated behavior: `PaymentGatewayClient.authorize` resolves to
  `HttpPaymentGatewayClient.authorize`; `HttpClient.post` is classified as an
  HTTP client boundary.
- Expected artifacts: `expected-flow.json`, `expected-flow.md`,
  `expected-flow.mmd`, `expected-context-pack.md`.

## Real Benchmark

Project:

```text
${WORKSPACE_ROOT}/aws-fintech-transfer-lab/onboarding
```

Entrypoint:

```text
com.study.onboarding.modules.auth.api.AuthController.register
```

Command:

```bash
./gradlew run --args="--project ${WORKSPACE_ROOT}/aws-fintech-transfer-lab/onboarding --entrypoint com.study.onboarding.modules.auth.api.AuthController.register"
```

Expected result:

- nodes: 23
- edges: 35
- resolutions: 2
- boundaries: 8
- unresolved: 0

Expected resolutions:

- `UserRegistrationInternal.create -> UserServiceImpl.create`
- `TransferServiceClient.notifyUserCreated -> TransferServiceClientImpl.notifyUserCreated`

Expected boundaries:

- `ResponseEntity.status`
- `UserRepository.findByEmail`
- `UserRepository.save`
- `PasswordEncoder.encode`
- `RestClient.post`
- `Logger.info`
- `Logger.warn`
- `MDC.get`

## Acceptance Criteria

- [x] `docs/graph-contract.md` points to the active contract.
- [x] `docs/phase-1-flow-graph-contract.md` documents FlowGraph 1.0.
- [x] Phase 1 examples exist.
- [x] Expected artifacts exist.
- [x] Expected artifacts match current analyzer output for the examples.
- [x] `./gradlew test` passes.
- [x] `./gradlew build` passes.
- [x] Core does not depend on PSI or adapters.
- [x] Real benchmark `AuthController.register` passes.

## Known Limitations

- No advanced data flow.
- No runtime behavior.
- No Kotlin support.
- No Neo4j persistence.
- No QA generation or runtime tracing.
- No complete resolution of `@Qualifier`, `@Profile`, or `@Primary`.
- Chain calls remain limited.
- Overloads, inheritance, lambdas, method references, and Lombok remain limited.

## Next Phase Candidate

The next candidate phase is:

- Phase 2 - Spring Entrypoints

Candidate Spring entrypoints:

- Controllers and endpoints.
- Listeners.
- Scheduled jobs.
- Consumers.
- Workers.
