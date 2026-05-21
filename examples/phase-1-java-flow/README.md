# Phase 1 Java Flow Fixtures

Esta pasta contem exemplos pequenos, versionados e controlados para validar a
Fase 1 Java Flow Graph MVP do Code Atlas.

## Objetivo

Os exemplos documentam comportamentos esperados do analyzer sem depender do
projeto real `aws-fintech-transfer-lab`. Eles foram derivados dos problemas
observados no fluxo `AuthController.register`: resolucao de receiver field,
interface para implementacao concreta, continuidade para metodos internos,
boundaries de repository/client/framework e registro explicito de unresolved.

## Relacao com a Fase 1

A Fase 1 precisa produzir um grafo deterministico e conservador a partir de um
entrypoint Java. Estes fixtures tornam explicitas as regras minimas esperadas
para:

- chamadas diretas;
- controller para service concreto;
- interface com implementacao unica;
- interface com multiplas implementacoes;
- repositories sem implementacao local;
- clients externos e boundaries.

`flow.json` e o contrato primario. `flow.md`, `flow.mmd`,
`context-pack.md` e `agent-handoff.md` sao derivacoes do mesmo `FlowGraph` e
nao devem introduzir fatos que nao existam no JSON.

Os arquivos em `code-atlas.expected/` sao fixtures de regressao. Eles
representam o comportamento aceito para a Fase 1 no schema atual
`FlowGraph 1.0`.

## Estrutura

Cada subpasta e um caso de uso isolado:

- `01-direct-method-call`
- `02-controller-service`
- `03-interface-single-implementation`
- `04-interface-multiple-implementations`
- `05-repository-boundary`
- `06-external-client-boundary`

Dentro de cada exemplo:

- `src/main/java/...` contem o codigo Java minimo.
- `README.md` descreve o nome, objetivo, entrypoint, fluxo esperado,
  comportamento validado e criterios de aceite.
- `code-atlas.expected/expected-flow.json` e o contrato primario.
- `code-atlas.expected/expected-flow.md`, `expected-flow.mmd` e
  `expected-context-pack.md` sao derivacoes do JSON esperado.

## Examples

| Example | Entrypoint | Validates | Expected result |
| --- | --- | --- | --- |
| `01-direct-method-call` | `com.example.direct.OrderController.create` | Direct call on concrete receiver and traversal into an internal method. | nodes=5, edges=5, resolutions=0, boundaries=0, unresolved=0 |
| `02-controller-service` | `com.example.controllerservice.CustomerController.register` | Controller to concrete service through a declared field. | nodes=6, edges=7, resolutions=0, boundaries=0, unresolved=0 |
| `03-interface-single-implementation` | `com.example.interfaces.single.RegistrationController.register` | Interface call resolved to one implementation plus repository boundary. | nodes=9, edges=10, resolutions=1, boundaries=1, unresolved=0 |
| `04-interface-multiple-implementations` | `com.example.interfaces.multiple.NotificationController.send` | Ambiguous interface dispatch recorded as unresolved with candidates. | nodes=8, edges=7, resolutions=0, boundaries=0, unresolved=1 |
| `05-repository-boundary` | `com.example.repository.AccountService.openAccount` | Repository methods without local implementation as boundaries. | nodes=5, edges=5, resolutions=0, boundaries=2, unresolved=0 |
| `06-external-client-boundary` | `com.example.externalclient.PaymentService.pay` | Single implementation resolution followed by HTTP client boundary. | nodes=8, edges=8, resolutions=1, boundaries=1, unresolved=0 |

## Execucao Manual

Exemplo:

```bash
./gradlew run --args="--project examples/phase-1-java-flow/03-interface-single-implementation --entrypoint com.example.interfaces.single.RegistrationController.register --output build/code-atlas-examples/03"
```

Ao omitir `--output`, o CLI grava os artefatos em `.code-atlas` dentro do
projeto analisado. Para fixtures, prefira usar `--output` apontando para
`build/...` durante validacoes manuais.

## Benchmark Real

O projeto real `aws-fintech-transfer-lab/onboarding` e validacao de integracao,
nao substituto dos fixtures. O benchmark real confirma que as regras pequenas
dos exemplos continuam funcionando em um fluxo mais proximo do uso real:

```bash
./gradlew run --args="--project /Users/eriveltomuller/Documents/GitHub/aws-fintech-transfer-lab/onboarding --entrypoint com.study.onboarding.modules.auth.api.AuthController.register"
```

Resultado aceito para a Fase 1:

- nodes: 23
- edges: 35
- resolutions: 2
- boundaries: 8
- unresolved: 0

Os fixtures continuam sendo o contrato de regressao pequeno e versionado. O
benchmark real deve ser tratado como validacao de integracao e sinal de que os
comportamentos combinados permanecem coerentes.

## Testes Automatizados

Os exemplos sao verificados pelos testes em `src/test/java`, que executam o
analyzer e conferem se os expected artifacts existem e seguem o schema atual.

Uma evolucao natural e ampliar essa cobertura para comparar o conteudo completo
de todos os artifacts gerados com os arquivos em `code-atlas.expected/`.

Nao ha artefatos atuais em `.code-atlas` nesta pasta de fixtures.
