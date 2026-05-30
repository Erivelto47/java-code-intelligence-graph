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
- blocks execution if any phase is marked `validation`;
- promotes the first `planned` phase to `next` when there is no `next` and no
  `validation`;
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
- no automatic transition to `implemented`, `approved` or `validation`;
- no Codex/model execution;
- no product implementation;
- no merge;
- no push.


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
4. Se existir qualquer fase com status validation, pare a execução e informe que há fase aguardando revisão humana.
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
3. Ler o prompt gerado em harness/bin/build/prompts/<phase-id>.codex-prompt.txt.
4. Ler o blueprint indicado no prompt.
5. Ler os derivados indicados no prompt:
   - handoff;
   - validation;
   - completion.
6. Implementar estritamente o escopo do blueprint.
7. Rodar todas as validações exigidas no prompt/blueprint.
8. Gerar o runtime report no path indicado pelo prompt.
9. Confirmar que o report está em harness/reports/runs/.
10. Confirmar que o report não está staged.
11. Confirmar que prompts temporários em harness/bin/build/ não estão staged.
12. Confirmar que não houve alterações fora do escopo.
13. Fazer commit apenas dos arquivos versionáveis da fase.
14. Após o commit da implementação, atualizar harness/phases/phase-index.tsv:
    - mudar o status da fase executada de next para implemented;
    - preencher a coluna commit com o hash curto do commit criado.
15. Fazer um segundo commit pequeno apenas para a atualização do phase index.
16. Não fazer push.

Validações mínimas obrigatórias, salvo se o prompt da fase exigir mais:

git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check

Também execute todos os comandos adicionais exigidos pelo blueprint/prompt da fase, especialmente regressões CLI e comparações de fixtures.

Commit da implementação:

Após validações passarem, faça commit com uma mensagem coerente com a fase.

Exemplo:

git commit -m "Add <phase summary>"

Atualização do phase index:

Depois do commit da implementação:

1. Obtenha o hash curto:

git rev-parse --short HEAD

2. Atualize harness/phases/phase-index.tsv para a fase executada:

- status: implemented
- commit: <hash curto>

3. Faça commit separado:

git add harness/phases/phase-index.tsv
git commit -m "Mark <phase-id> as implemented"

Não fazer push automaticamente.

Ao final:

1. Informe o commit da implementação.
2. Informe o commit de atualização do phase index.
3. Informe o path do report gerado.
4. Informe os principais arquivos alterados.
5. Informe os testes executados e resultados.
6. Informe explicitamente o que ficou fora de escopo.
7. Informe que a fase está marcada como implemented e ainda depende de revisão humana/web antes de merge/push.

Importante:

Não marque a fase como approved automaticamente.

implemented significa que a fase foi implementada e commitada.
approved continua sendo decisão do Human Reviewer após revisão do report.
```
