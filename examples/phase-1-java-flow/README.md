# Phase 1 Java Flow Fixtures

Esta pasta contem exemplos pequenos, versionados e controlados para validar a Fase 1 Java Flow Graph MVP do Code Atlas.

## Objetivo

Os exemplos documentam comportamentos esperados do analyzer sem depender temporariamente do projeto real `aws-fintech-transfer-lab`. Eles foram derivados dos problemas observados no fluxo `AuthController.register`: resolucao de receiver field, interface para implementacao concreta, continuidade para metodos internos, boundaries de repository/client/framework e registro explicito de unresolved.

## Como Usar

Cada subpasta e um caso de uso isolado:

- `01-direct-method-call`
- `02-controller-service`
- `03-interface-single-implementation`
- `04-interface-multiple-implementations`
- `05-repository-boundary`
- `06-external-client-boundary`

Dentro de cada exemplo:

- `src/main/java/...` contem o codigo Java minimo.
- `README.md` descreve o objetivo, entrypoint, fluxo esperado e criterios de aceite.
- `code-atlas.expected/expected-flow.json` e o contrato primario.
- `code-atlas.expected/expected-flow.md`, `expected-flow.mmd` e `expected-context-pack.md` sao derivacoes semanticas do JSON esperado.

Os arquivos `expected-*` representam o comportamento aceito para a Fase 1 no
schema atual do `FlowGraph`. `expected-flow.json` e o contrato primario; os
demais arquivos sao derivacoes geradas a partir do mesmo grafo.

## Relacao com a Fase 1

A Fase 1 precisa produzir um grafo deterministico e conservador a partir de um entrypoint Java. Estes fixtures tornam explicitas as regras minimas esperadas para:

- chamadas diretas;
- controller para service concreto;
- interface com implementacao unica;
- interface com multiplas implementacoes;
- repositories sem implementacao local;
- clients externos e boundaries.

Fatos deterministicos aparecem como `FACT`. Resolucao por convencao estrutural, como interface com implementacao unica local, aparece como `INFERRED`. Quando a resolucao nao for deterministica, o contrato usa `unresolved` em vez de inventar uma edge.

## Testes Automatizados

O projeto real `aws-fintech-transfer-lab` deve ser usado depois como validacao de integracao. Estes exemplos sao a base menor e controlada que deve falhar/passsar antes de ampliar o escopo para o projeto real.

Os exemplos sao verificados pelos testes em `src/test/java`, que executam o
analyzer e conferem se os expected artifacts seguem o schema atual.

Uma evolucao natural e ampliar essa cobertura para comparar o conteudo completo
do `flow.json` gerado com `code-atlas.expected/expected-flow.json` em cada
fixture.

Nao ha artefatos atuais em `.code-atlas` nesta pasta de fixtures.

## Execucao Manual

Exemplo:

```bash
./gradlew run --args="--project examples/phase-1-java-flow/03-interface-single-implementation --entrypoint com.example.interfaces.single.RegistrationController.register --output build/code-atlas-examples/03"
```

Ao omitir `--output`, o CLI grava os artefatos em `.code-atlas` dentro do
projeto analisado. Para fixtures, prefira usar `--output` apontando para
`build/...` durante validacoes manuais.
