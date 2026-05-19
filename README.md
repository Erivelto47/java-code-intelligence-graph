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

Entrada:

```bash
./gradlew run --args="--project ./repo --entrypoint com.company.FooService.method"
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
  adapter/source/      Analyzer textual determinístico para arquivos .java
  adapter/psi/         Adapter futuro para IntelliJ PSI
  output/              Geradores derivados: JSON, Markdown, Mermaid, context pack
docs/
  phase-1.md
  graph-contract.md
examples/
  sample-flow.json
```

## Desenvolvimento

Rodar testes:

```bash
./gradlew test
```

Gerar artefatos com o analyzer textual real:

```bash
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder"
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
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder --output build/code-atlas-output"
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
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder"
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

O MVP atual resolve um entrypoint classe/método em código fonte Java, cria um grafo determinístico com chamadas diretas simples e gera os quatro artefatos derivados. A análise PSI ainda não foi implementada.
