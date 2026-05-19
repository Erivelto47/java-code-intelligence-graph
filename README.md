# Java Code Intelligence Graph

Ferramenta para analisar codebases Java a partir de um ponto de entrada e gerar artefatos estruturados de entendimento técnico.

## Objetivo

Transformar código Java existente em grafos técnicos úteis para:

- entendimento de fluxos;
- Spec-Driven Development;
- Harness Engineering;
- chat com IA;
- análise de impacto;
- documentação;
- evolução arquitetural;
- futura identificação de fronteiras arquiteturais.

## Fase atual

**Fase 1: Java Flow Graph MVP**

Entrada planejada:

```bash
analyze-flow --project ./repo --entrypoint com.company.FooService.method
```

Saídas planejadas:

```text
flow.json
flow.md
flow.mmd
context-pack.md
```

## Princípios do projeto

1. O core não depende de IntelliJ PSI.
2. IntelliJ PSI é apenas um adapter.
3. JSON é a saída primária.
4. Markdown, Mermaid e context packs são derivados.
5. Fatos determinísticos e interpretações de IA devem ser separados.
6. A entrada principal é um nó: classe/método, endpoint, listener ou job.
7. A Fase 1 foca em Java com entrypoint classe/método.
8. Kotlin, QA, Neo4j, runtime tracing e data flow avançado ficam fora da Fase 1.

## Estrutura inicial

```text
src/main/java/com/codeatlas/
  cli/                 CLI analyze-flow
  core/                Modelo e contratos independentes de PSI
  adapter/psi/         Adapter futuro para IntelliJ PSI
  output/              Geradores derivados: JSON, Markdown, Mermaid, context pack
docs/
  phase-1.md
  graph-contract.md
examples/
  sample-flow.json
```

## Status

Repositório inicial criado para começar a implementação do Java Flow Graph MVP.
