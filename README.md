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

**Fase 1: Java Flow Graph MVP** esta fechada para o MVP de grafo de fluxo por
entrypoint Java.

Entrada por metodo Java:

```bash
./gradlew run --args="analyze-flow --project ./repo --entrypoint com.company.FooService.method"
```

Saídas:

```text
.code-atlas/project-index.json
.code-atlas/flows-index.md
.code-atlas/flows/<package-path>/<ClassName>/<methodName>/flow.json
.code-atlas/flows/<package-path>/<ClassName>/<methodName>/flow.md
.code-atlas/flows/<package-path>/<ClassName>/<methodName>/flow.mmd
.code-atlas/flows/<package-path>/<ClassName>/<methodName>/context-pack.md
.code-atlas/flows/<package-path>/<ClassName>/<methodName>/agent-handoff.md
```

**Fase 2: Spring Entrypoints** esta fechada para os MVPs 2.1 e 2.2:

```bash
./gradlew run --args="list-endpoints --project ./repo"
./gradlew run --args="analyze-flow --project ./repo --endpoint 'POST /auth/register'"
```

O contrato da Fase 2 fica documentado em
`docs/phase-2-entrypoints-contract.md`, e a visao geral da fase fica em
`docs/phase-2-spring-entrypoints.md`.

**Fase 3: Project Index** esta fechada para o inventario estrutural
deterministico do projeto:

```bash
./gradlew run --args="index-project --project ./repo"
```

O contrato da Fase 3 fica documentado em
`docs/phase-3-project-index-contract.md`.

**Fase 4: Decision Trace** inicia com contrato e fixtures em
`docs/phase-4-decision-trace-contract.md` e
`examples/phase-4-decision-trace/`. A Fase 4 deve gerar artefatos
deterministicos separados para decisoes, validacoes, throws condicionais,
returns antecipados e outcomes.

Comando MVP da Fase 4.1:

```bash
./gradlew run --args="analyze-decisions --project ./repo --entrypoint com.company.FooService.method"
```

**Fase 5: AI Interpretation Layer** fica reservada para consumir Project Index,
Flow Graph e Decision Trace sem misturar interpretacao de IA nos artefatos
deterministicos.

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
  cli/                 CLI analyze-flow, analyze-decisions e comandos auxiliares
  application/decision/ Orquestração de Decision Trace por adapter de linguagem
  core/                Modelo e contratos independentes de PSI
  core/decision/       Contrato comum e linguagem-agnóstico de Decision Trace
  adapter/java/source/decision/
                       Adapter source-text Java para Decision Trace
  adapter/source/      Analyzer textual determinístico para arquivos .java
  adapter/psi/         Adapter futuro para IntelliJ PSI
  output/              Geradores derivados: JSON, Markdown, Mermaid, context pack
  output/decision/     Writers de Decision Trace por formato
docs/
  phase-1.md
  graph-contract.md
  phase-2-spring-entrypoints.md
  phase-2-entrypoints-contract.md
  phase-3-project-index-contract.md
  phase-4-decision-trace-contract.md
examples/
  sample-flow.json
  phase-2-spring-entrypoints/
  phase-4-decision-trace/
```

## Desenvolvimento

Rodar testes:

```bash
./gradlew test
```

Gerar artefatos com o analyzer textual real:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

Gerar artefatos com o analyzer stub:

```bash
./gradlew run --args="--project ./ --entrypoint com.company.FooService.method --stub"
```

Por padrão, os arquivos são escritos em:

```text
<projectPath>/.code-atlas/flows/<package-path>/<ClassName>/<methodName>/
```

Para o exemplo acima:

```text
examples/java-simple/.code-atlas/flows/com/company/FooService/processOrder/
```

Também são escritos dois índices no projeto analisado:

```text
<projectPath>/.code-atlas/project-index.json
<projectPath>/.code-atlas/flows-index.md
```

Esses índices ajudam agentes que conseguem ler arquivos por caminho exato, mas não conseguem navegar livremente pela árvore do repositório. O `project-index.json` é o índice estruturado com o flow atual, arquivos fonte e caminhos dos artefatos. O `flows-index.md` é a versão humana para localizar rapidamente o flow, o context pack e o JSON primário.

Use `--output` para sobrescrever explicitamente esse destino:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder --output build/code-atlas-output"
```

Com `--output`, os artefatos do flow continuam sendo gravados no diretório informado. Nessa forma, a CLI também grava `agent-handoff.md` no diretório de output, mas não grava obrigatoriamente `project-index.json` ou `flows-index.md` em `.code-atlas`.

Exemplo de estrutura gerada sem `--output`:

```text
examples/java-simple/.code-atlas/
  project-index.json
  flows-index.md
  flows/com/company/FooService/processOrder/
    flow.json
    flow.md
    flow.mmd
    context-pack.md
    agent-handoff.md
```

O `agent-handoff.md` resume o repositório, o projeto, o entrypoint, os arquivos fonte, os artefatos gerados, a contagem de nós e arestas, e os nós/arestas detectados. Ele pode conter orientação operacional para outros agentes, mas não adiciona interpretação de IA.

## SourceTextFlowAnalyzer

O analyzer padrão da Fase 1 lê arquivos `.java` diretamente e faz parsing textual conservador, sem IntelliJ PSI, JavaParser, Spoon ou outra biblioteca externa de parsing.

Exemplo analisado:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

O `flow.json` gerado contém nós como:

```text
class:com.company.FooService
method:com.company.FooService.processOrder
method:com.company.FooService.validate
method:repository.save
method:paymentClient.charge
method:com.company.FooService.mapper
```

Limitações explícitas desta fase:

- Detecta apenas chamadas diretas simples no corpo do método de entrada.
- Não resolve overloads, polimorfismo, herança, imports complexos ou tipos de campos.
- Não detecta construtores, reflection, lambdas, method references ou cadeias complexas como `a.b().c()`.
- Quando o alvo não é resolvido localmente, o grafo usa nomes parciais/inferidos com fatos marcados como determinísticos.

## Status

A Fase 1 resolve um entrypoint classe/método em código fonte Java, cria um grafo determinístico com chamadas diretas simples e gera os artefatos derivados de flow. A Fase 2.1 + 2.2 adiciona descoberta e listagem source-text de endpoints Spring MVC e resolução de `analyze-flow --endpoint` para `javaEntrypoint`. A Fase 3 adiciona Project Index, `entrypoints.json`, `flows-index.md`, hints e diagnosticos de uso do indice no flow. A Fase 4 esta definida como Decision Trace deterministico. A Fase 5 fica reservada para interpretacao de IA. A análise PSI ainda não foi implementada.
