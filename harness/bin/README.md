# Harness Binaries

This directory contains small operational scripts for the harness.

## run-phase.sh

Use the runner after the Human Reviewer approves a primary blueprint:

```bash
./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

The runner derives the phase id, expected handoff, validation, completion and
runtime report paths, then creates missing derived artifacts from the harness
templates. Existing derived artifacts are left unchanged by default.

Use dry-run mode to inspect paths and branch/status checks without creating
files:

```bash
./harness/bin/run-phase.sh --dry-run harness/blueprints/<phase>.blueprint.md
```

The runner blocks execution on `master` by default. A Human Reviewer can allow
that explicitly for exceptional cases:

```bash
HARNESS_ALLOW_MASTER=1 ./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

The runner prepares operational artifacts only. It does not implement product
scope, execute a phase autonomously, call models, push, merge or write the
runtime report.

## run-next-phase.sh

Use the next phase runner to synchronize the phase queue from blueprints,
derive paths from phase ids and prepare the next executable phase:

```bash
./harness/bin/run-next-phase.sh
```

The runner:

- reads `harness/blueprints/*.blueprint.md`;
- synchronizes `harness/phases/phase-index.tsv`;
- keeps existing statuses and commits;
- adds blueprint ids missing from the index as `planned`;
- derives blueprint, handoff, validation, completion, report and prompt paths
  from the phase id;
- validates allowed statuses;
- fails if more than one phase is marked `next`;
- blocks execution if any phase is marked `in_progress`;
- blocks execution if any phase is marked `validation`;
- promotes the first `planned` phase to `next` when there is no `next`,
  `in_progress` or `validation`;
- blocks if the derived report for the `next` phase already exists;
- calls `./harness/bin/run-phase.sh <blueprint>`;
- generates `harness/bin/build/prompts/<phase-id>.codex-prompt.txt`;
- prints the branch, phase id, derived paths, report path, prompt path and next
  step.

Dry-run mode delegates to `run-phase.sh --dry-run` and still renders the
temporary Codex prompt under `harness/bin/build/`. It also synchronizes the
phase index, because the queue itself is versioned harness state:

```bash
./harness/bin/run-next-phase.sh --dry-run
```

Limitations:

- no `--phase` override in the MVP;
- no automatic transition to `implemented` or `approved`;
- no Codex/model execution;
- no product implementation;
- no merge;
- no push.

## update-phase-index-status.sh

Use the phase index status helper inside a Codex execution session to apply
the only automatic lifecycle transitions allowed before human closeout:

```bash
./harness/bin/update-phase-index-status.sh start <phase-id>
./harness/bin/update-phase-index-status.sh validation <phase-id>
```

The helper preserves the TSV format and row order. It only supports:

- `start`: `next` -> `in_progress`, with commit forced to `TBD`;
- `validation`: `in_progress` -> `validation`, with commit forced to `TBD`.

The `validation` transition requires the derived report to already exist under
`harness/reports/runs/`. The helper never marks a phase as `implemented`, never
writes a commit hash and never promotes another phase to `next`.


# Default prompt to codex

```text
Você está atuando como Codex CLI Executor no projeto Java Code Intelligence Graph / Code Atlas.

Repositório local:

./java-code-intelligence-graph

Objetivo:

Executar a próxima fase definida pelo harness, usando o fluxo blueprint-driven.

Fonte da fila de fases:

harness/phases/phase-index.tsv

Regras principais:

1. Leia harness/phases/phase-index.tsv.
2. Encontre a fase com status next.
3. Confirme que existe exatamente uma fase com status next.
4. Se existir qualquer fase com status validation ou in_progress, pare a execução e informe que há fase aguardando revisão humana ou ainda em andamento.
5. Use o id da fase next para localizar o prompt gerado pelo harness em:

harness/bin/build/prompts/<phase-id>.codex-prompt.txt

6. Leia completamente esse prompt gerado.
7. Execute a fase seguindo esse prompt.
8. O blueprint citado no prompt é a fonte primária da fase.
9. Handoff, validation e completion são derivados de apoio operacional.
10. Se algum derivado divergir do blueprint, corrija o derivado para refletir o blueprint.
11. Não use fases antigas como fonte se o phase-index.tsv apontar outra fase como next.

Antes de implementar:

Execute:

git status
git branch --show-current

Confirme:

- não está em master;
- não fará checkout para master;
- não criará branch a partir de master;
- não fará merge para master;
- não fará push automático;
- o makefile untracked, se existir, deve permanecer intocado;
- prompts em harness/bin/build/ não devem ser versionados;
- reports em harness/reports/runs/ não devem ser versionados.

Fluxo obrigatório:

1. Ler harness/phases/phase-index.tsv.
2. Identificar a fase next.
3. Confirmar que nao existe fase validation ou in_progress.
4. Atualizar a fase executada de next para in_progress, mantendo commit TBD:

./harness/bin/update-phase-index-status.sh start <phase-id>

5. Ler o prompt gerado em harness/bin/build/prompts/<phase-id>.codex-prompt.txt.
6. Ler o blueprint indicado no prompt.
7. Ler os derivados indicados no prompt:
   - handoff;
   - validation;
   - completion.
8. Implementar estritamente o escopo do blueprint.
9. Rodar todas as validações exigidas no prompt/blueprint.
10. Gerar o runtime report no path indicado pelo prompt.
11. Confirmar que o report está em harness/reports/runs/.
12. Atualizar a fase executada de in_progress para validation, mantendo commit TBD:

./harness/bin/update-phase-index-status.sh validation <phase-id>

13. Confirmar que o report não está staged.
14. Confirmar que prompts temporários em harness/bin/build/ não estão staged.
15. Confirmar que não houve alterações fora do escopo.
16. Não marcar implemented.
17. Não preencher hash de commit no phase-index.tsv.
18. Não promover outra fase para next.
19. Não fazer push.

Validações mínimas obrigatórias, salvo se o prompt da fase exigir mais:

git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check

Também execute todos os comandos adicionais exigidos pelo blueprint/prompt da fase, especialmente regressões CLI e comparações de fixtures.

Commit e fechamento:

Nao faca commit a menos que o usuario solicite explicitamente nesta execucao.
O estado `implemented` so pode ser aplicado depois de revisao/aprovacao humana
e depois de existir commit real. Ate la, a fase concluida pelo Codex deve ficar
como `validation` com commit `TBD`.

Ao final:

1. Informe o path do report gerado.
2. Informe os principais arquivos alterados.
3. Informe os testes executados e resultados.
4. Informe explicitamente o que ficou fora de escopo.
5. Informe que a fase está marcada como validation/TBD e depende de revisão humana antes de commit, implemented, merge ou push.

Importante:

Não marque a fase como approved automaticamente.

implemented significa que a fase foi aprovada e commitada.
approved continua sendo decisão do Human Reviewer após revisão do report.
```
