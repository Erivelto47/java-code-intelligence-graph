# Agentic Harness do Code Atlas

O `harness/` e o protocolo operacional versionado do Code Atlas para fases
guiadas por blueprint como entrada primaria. Ele organiza como uma intencao
tecnica vira blueprint aprovado, artefatos derivados, execucao no repositorio,
validacao, report e decisao humana de continuidade.

Este harness nao executa codigo por si so. Ele e uma estrutura simples de
Markdown e arquivos leves para preservar contexto entre ChatGPT Web, Codex CLI
ou qualquer ferramenta equivalente.

O harness usa "agentic workflow" como nome do estilo de colaboracao. Os atores
operacionais sao documentados como roles: responsabilidades que podem ser
executadas por humanos, IA assistida ou ferramentas.

## Por que existe

O projeto evolui em fases com contratos, fixtures e artefatos deterministas.
Sem um protocolo operacional, cada fase depende de prompts soltos e memoria de
chat. O harness reduz esse risco registrando:

- blueprints como fonte primaria da fase;
- handoffs, validation checklists e completion criteria derivados do blueprint;
- reports depois da execucao;
- estado atual e validacoes;
- criterios objetivos de completion;
- decisoes operacionais sobre branch, merge, reports e artefatos.

## Politica blueprint-driven

O blueprint e a entrada primaria e a fonte de verdade da fase.

```text
Primary input:
harness/blueprints/<phase>.blueprint.md

Derived artifacts:
harness/handoffs/<phase>.handoff.md
harness/validations/<phase>.validation.md
harness/completion/<phase>.completion.md

Runtime output:
harness/reports/runs/<phase-report>.md
```

O blueprint deve ser criado pelo ChatGPT Web Architect e aprovado pelo Human
Reviewer antes da execucao. Handoff, validation e completion podem ser
gerados ou atualizados pelo Codex CLI Executor a partir do blueprint, dos
templates do harness e dos padroes do projeto. Eles sao revisaveis e podem ser
versionados, mas nao sao fontes independentes de verdade.

Se algum derivado divergir do blueprint, a execucao deve pausar para corrigir
o derivado. Uma fase nao deve ser executada apenas a partir de handoff sem
blueprint correspondente aprovado.

## Blueprint runner skeleton

Harness 0.3 adiciona um runner simples para preparar artefatos operacionais a
partir de um blueprint aprovado:

```bash
./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

O blueprint continua sendo o input primario e a fonte de verdade. O runner
deriva o phase id a partir de `harness/blueprints/<phase>.blueprint.md`,
calcula os paths esperados e cria, quando ausentes:

- `harness/handoffs/<phase>.handoff.md`
- `harness/validations/<phase>.validation.md`
- `harness/completion/<phase>.completion.md`

O runtime report path sempre aponta para `harness/reports/runs/`, por exemplo:

```text
harness/reports/runs/PHASE_4_2_JAVA_DECISION_UNRESOLVED_EARLY_RETURN_REPORT.md
```

Arquivos derivados existentes sao preservados por padrao. O runner imprime
branch, `git status --short`, paths derivados e o proximo passo. Ele bloqueia
`master` por padrao; o override `HARNESS_ALLOW_MASTER=1` deve ser usado apenas
com aprovacao humana explicita.

Modo dry-run:

```bash
./harness/bin/run-phase.sh --dry-run harness/blueprints/<phase>.blueprint.md
```

Limitacao atual: o runner nao executa fases autonomamente, nao implementa
escopo de produto, nao chama modelos, nao faz push, nao faz merge e nao escreve
o report runtime. Ele apenas prepara o pacote operacional derivado.

## Next phase runner

Harness 0.4 adiciona um runner de alto nivel para descobrir a proxima fase a
partir do indice versionado:

```bash
./harness/bin/run-next-phase.sh
```

O indice fica em:

```text
harness/phases/phase-index.tsv
```

O runner valida que existe exatamente uma fase com status `next`, confirma que
o blueprint dessa fase existe, chama `run-phase.sh` para preparar handoff,
validation e completion, e gera um prompt padrao para Codex em:

```text
harness/bin/build/prompts/<phase-id>.codex-prompt.txt
```

Modo dry-run:

```bash
./harness/bin/run-next-phase.sh --dry-run
```

`run-phase.sh` prepara uma fase especifica informada pelo usuario.
`run-next-phase.sh` descobre a proxima fase pelo indice e reutiliza
`run-phase.sh`. Nenhum dos dois executa Codex automaticamente, implementa
produto, altera status no indice, faz merge ou faz push.

## Roles operacionais

### ChatGPT Web Architect

Transforma a intencao do usuario em blueprint tecnico, define escopo e fora de
escopo, revisa reports, pode revisar artefatos derivados e sugere a proxima
etapa. Nao executa mudancas diretamente no repositorio.

### Codex CLI Executor

Le o blueprint primario, deriva ou atualiza handoff, validation e completion
quando solicitado, executa no repositorio dentro do escopo aprovado, roda
validacoes, gera report factual, registra riscos e limitacoes, e nao altera
`master`, faz merge ou push sem instrucao explicita.

### Human Reviewer

Aprova o blueprint antes da execucao, decide se os derivados estao aceitaveis,
aprova ou rejeita o report, decide push, merge e ajustes de escopo, e
interrompe a execucao se branch, escopo ou validacoes estiverem errados. E uma
role humana, nao um agente autonomo.

## Como executar uma fase

1. Registrar ou revisar o blueprint em `harness/blueprints/`.
2. Obter aprovacao humana do blueprint antes de executar a fase.
3. Usar `./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md`
   para derivar handoff, validation checklist, completion criteria e report
   path a partir do blueprint e dos templates do harness.
4. Verificar `git status` e `git branch --show-current` antes de alterar
   arquivos.
5. Executar a implementacao somente na branch de trabalho da fase, usando o
   blueprint como fonte primaria e os derivados como apoio operacional.
6. Rodar as validacoes derivadas do blueprint.
7. Gerar report factual em `harness/reports/runs/`.
8. Enviar o report para revisao humana ou para o Architect.
9. Decidir a proxima etapa somente depois da revisao.

O workflow completo fica em
`harness/workflows/phase-execution.workflow.md`.

## Politica de branches

- A branch deve ser verificada antes de qualquer alteracao.
- No ciclo atual, nao criar branches a partir de `master` a menos que o Human
  Reviewer declare explicitamente que a branch de trabalho foi consolidada.
- Durante consolidacao da Fase 4 / harness, criar branches a partir da branch
  de trabalho atual.
- Se uma microfase precisar de isolamento, criar a branch a partir da branch de
  trabalho atual.
- Merge para `master` somente depois de aprovacao humana do harness e da Fase 4
  consolidados.
- Nao fazer merge para `master` sem decisao humana explicita.
- Nao fazer push automatico.

## Politica de reports

- Reports temporarios reais gerados por executor devem ficar em
  `harness/reports/runs/`.
- `harness/reports/` fica reservado para politica versionada, marcadores de
  diretorio e templates.
- Temporary real reports must be generated under `harness/reports/runs/`.
- Root-level `harness/reports/` is reserved for report policy, directory
  markers and versioned templates.
- `harness/reports/` existe no Git via `.gitkeep`, `README.md` e
  `templates/`.
- Reports temporarios em `harness/reports/runs/` sao ignorados pelo Git, salvo
  decisao explicita de versionar um report especifico.
- Templates, READMEs, `.gitkeep` e convencoes do harness permanecem
  versionados.
- Reports anexados ao chat podem apoiar revisao, mas nao sao necessariamente
  artefatos permanentes do repositorio.
- Artefatos de fixture ou examples podem ser versionados quando fizerem parte
  de um contrato do produto.

## Conexao com o Code Atlas

O harness organiza a execucao do projeto, mas nao substitui os contratos do
produto. Decision Trace, Flow Graph, Project Index e outros artefatos continuam
pertencendo ao dominio do Code Atlas.

Em especial, `harness/decisions/` registra decisoes operacionais do processo.
Ele nao e equivalente a `.code-atlas/decisions/`, aos pacotes
`com.codeatlas.core.decision` ou aos artefatos de Decision Trace.

## O que o harness nao substitui

- testes automatizados;
- revisao humana;
- contratos de produto;
- validacao por build;
- controle consciente de branch, commit, push e merge.

## Estrutura

```text
harness/
  bin/            Scripts operacionais leves do harness.
  workflows/      Workflows operacionais versionados.
  roles/          Roles e responsabilidades operacionais.
  blueprints/     Entradas primarias e fontes de verdade das fases.
  handoffs/       Artefatos derivados para transferencia operacional.
  phases/         Indice versionado de fases e status operacionais.
  reports/        Politica, marcadores, templates e runs ignorados.
  state/          Templates de estado de execucao.
  validations/    Checklists derivados de validacao.
  completion/     Criterios derivados de conclusao.
  prompts/        Convencoes para prompts reutilizaveis.
  decisions/      Decisoes operacionais do harness.
```
