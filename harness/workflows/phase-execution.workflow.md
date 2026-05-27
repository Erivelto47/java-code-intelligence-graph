# Phase Execution Workflow

## Objetivo

Padronizar a execucao de uma fase do Code Atlas guiada por blueprint, mantendo
rastreabilidade entre intencao, branch, implementacao, validacao, report e
decisao humana.

## Roles

- ChatGPT Web Architect: transforma intencao em blueprint tecnico, define
  escopo e fora de escopo, revisa reports, pode revisar derivados e nao executa
  alteracoes diretamente no repositorio.
- Codex CLI Executor: le o blueprint primario, deriva ou atualiza handoff,
  validation e completion quando solicitado, executa no repositorio, roda
  validacoes, gera report e registra riscos sem extrapolar o escopo.
- Human Reviewer: role humana que aprova o blueprint antes da execucao, decide
  se derivados estao aceitaveis, aprova ou rejeita report, decide push, merge e
  ajustes, e interrompe a execucao se branch ou escopo estiverem errados.

## Sequencia de roles

1. Human Reviewer / requester inicia a intencao.
2. ChatGPT Web Architect transforma a intencao em blueprint.
3. Human Reviewer revisa e aprova o blueprint.
4. Codex CLI Executor deriva ou atualiza handoff, validation e completion.
5. Codex CLI Executor executa no repositorio usando blueprint como fonte
   primaria e derivados como apoio operacional.
6. Codex CLI Executor gera report factual.
7. ChatGPT Web Architect pode revisar o report e sugerir proxima etapa.
8. Human Reviewer decide aprovar, iterar, fazer push, fazer merge, pausar ou
   alterar escopo.

## Entradas

- Intencao do usuario ou objetivo da fase.
- Blueprint aprovado em `harness/blueprints/<phase>.blueprint.md`.
- Estado atual da branch de trabalho.
- Artefatos derivados, quando ja existirem:
  `harness/handoffs/<phase>.handoff.md`,
  `harness/validations/<phase>.validation.md` e
  `harness/completion/<phase>.completion.md`.
- Restricoes de escopo e fora de escopo definidas pelo blueprint.
- Checklist de validacao derivado do blueprint.
- Politica de reports da fase.

## Saidas

- Mudancas versionaveis no repositorio, quando houver.
- Handoff, validation checklist e completion criteria derivados ou atualizados,
  quando solicitado.
- Validacoes executadas e seus resultados.
- Report factual em `harness/reports/runs/`.
- Handoff ou estado atualizado, quando necessario.
- Recomendacao de proxima etapa para revisao humana.

## Regras de precedencia

- A execucao pode comecar apenas quando ha blueprint aprovado.
- Handoff, validation e completion sao derivados do blueprint.
- Se derivados e blueprint divergirem, pausar e corrigir os derivados antes de
  executar.
- Nao executar uma fase baseada apenas em handoff sem blueprint correspondente.
- Durante o ciclo atual, nao criar branch a partir de `master` sem aprovacao
  humana explicita.

## Ciclo minimo

### 1. Intake

Registrar a intencao do usuario, a fase pretendida, o objetivo principal e as
restricoes explicitas.

### 2. Blueprint creation

O Architect define escopo, fora de escopo, criterios de aceite, artefatos
esperados, validacoes, riscos conhecidos e criterios de sucesso no blueprint
primario.

### 3. Human blueprint approval

O Human Reviewer revisa o blueprint e aprova, rejeita ou pede ajuste. A fase
nao deve avancar para execucao sem essa aprovacao.

### 4. Derived package generation

O Codex CLI Executor, guiado pelo harness, gera ou atualiza:

- `harness/handoffs/<phase>.handoff.md`
- `harness/validations/<phase>.validation.md`
- `harness/completion/<phase>.completion.md`

Esses artefatos devem ser derivados do blueprint, dos templates do harness, dos
padroes do projeto e de validacoes existentes. Eles traduzem o blueprint em
instrucoes operacionais, mas nao podem ampliar escopo. Se houver conflito, o
blueprint vence e os derivados devem ser corrigidos.

### 5. Branch check

Antes de alterar qualquer arquivo, o Executor deve rodar:

```bash
git status
git branch --show-current
```

Regras:

- confirmar a branch ativa;
- no ciclo atual, nao criar branches a partir de `master` a menos que o Human
  Reviewer declare explicitamente que a branch de trabalho foi consolidada;
- durante consolidacao da Fase 4 / harness, criar novas branches a partir da
  branch de trabalho atual;
- se uma microfase precisar de isolamento, criar branch a partir da branch
  atual, nao da `master`;
- merge para `master` somente depois de aprovacao humana do harness e da Fase 4
  consolidados;
- nao fazer merge para `master` sem decisao humana;
- nao fazer push automatico.

### 6. Implementation

O Executor aplica apenas as mudancas dentro do escopo do blueprint. Handoff,
validation e completion servem como apoio operacional. Qualquer divergencia
entre derivados e blueprint deve interromper a execucao ate que os derivados
sejam corrigidos.

### 7. Validation

O Executor roda os comandos derivados do blueprint e definidos para a fase. Para
fases Java, o checklist padrao inclui:

```bash
./gradlew test
./gradlew build
git diff --check
```

Falhas ou checks nao executados devem ser registrados com detalhe.

### 8. Report generation

O Executor gera report factual em `harness/reports/runs/`. Reports
temporarios reais sao ignorados pelo Git por padrao. Templates, READMEs e
marcadores continuam versionados em `harness/reports/`.

Exemplo:

```text
harness/reports/runs/PHASE_4_2_JAVA_DECISION_UNRESOLVED_EARLY_RETURN_REPORT.md
```

O report deve registrar arquivos alterados, validacoes, resultados, riscos,
pendencias, fora de escopo e proxima etapa sugerida.

### 9. Review

O Architect pode revisar o report. O Human Reviewer avalia blueprint, derivados,
diff, validacoes e report. A fase so deve ser considerada completa depois dessa
revisao.

### 10. Next-step decision

O Human Reviewer decide se aprova, pede ajustes, faz push, faz merge, pausa,
altera escopo ou inicia a proxima fase. A decisao de merge para `master` nunca
e automatica.

## Politica de reports

- Reports temporarios reais de Codex devem ficar em `harness/reports/runs/`.
- Root-level `harness/reports/` e reservado para politica de reports,
  marcadores de diretorio e templates versionados.
- Temporary real reports must be generated under `harness/reports/runs/`.
- Root-level `harness/reports/` is reserved for report policy, directory
  markers and versioned templates.
- Reports em `harness/reports/runs/` sao ignorados pelo Git por padrao.
- `harness/reports/.gitkeep`, `README.md` e templates podem ser versionados.
- Reports de fase podem ser versionados somente por decisao explicita.
- Reports anexados ao chat podem apoiar revisao, mas nao sao necessariamente
  artefatos permanentes do repositorio.
- Artefatos de fixture e examples podem ser versionados quando forem parte do
  contrato da fase.

## Criterios de completion do workflow

- Branch verificada antes da execucao.
- Escopo e fora de escopo respeitados.
- Mudancas esperadas aplicadas.
- Validacoes executadas ou justificadas.
- Report gerado.
- Riscos e limitacoes registrados.
- Revisao humana habilitada por informacao suficiente.
