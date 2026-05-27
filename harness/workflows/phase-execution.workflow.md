# Phase Execution Workflow

## Objetivo

Padronizar a execucao de uma fase do Code Atlas guiada por blueprint, mantendo
rastreabilidade entre intencao, branch, implementacao, validacao, report e
decisao humana.

## Papeis

- ChatGPT Web Architect: transforma intencao em blueprint tecnico, define
  escopo e fora de escopo, gera prompt de execucao e revisa o report.
- Codex CLI Executor: executa no repositorio, aplica mudancas, roda validacoes,
  gera report e registra riscos sem extrapolar o escopo.
- Human Reviewer: aprova a fase, decide push, merge e ajustes, e interrompe a
  execucao se branch ou escopo estiverem errados.

## Entradas

- Intencao do usuario ou objetivo da fase.
- Estado atual da branch de trabalho.
- Blueprint ou prompt de handoff.
- Restricoes de escopo e fora de escopo.
- Checklist de validacao esperado.
- Politica de reports da fase.

## Saidas

- Mudancas versionaveis no repositorio, quando houver.
- Validacoes executadas e seus resultados.
- Report factual em `harness/reports/`.
- Handoff ou estado atualizado, quando necessario.
- Recomendacao de proxima etapa para revisao humana.

## Ciclo minimo

### 1. Intake

Registrar a intencao do usuario, a fase pretendida, o objetivo principal e as
restricoes explicitas.

### 2. Blueprint

O Architect define escopo, fora de escopo, criterios de aceite, artefatos
esperados, validacoes e riscos conhecidos.

### 3. Prompt handoff

O Architect ou Human Reviewer prepara um prompt para o Executor contendo
contexto, branch strategy, arquivos esperados, validacoes e report requerido.

### 4. Branch check

Antes de alterar qualquer arquivo, o Executor deve rodar:

```bash
git status
git branch --show-current
```

Regras:

- confirmar a branch ativa;
- nao voltar para `master` durante ciclos que devem continuar em branch de
  trabalho;
- se uma microfase precisar de isolamento, criar branch a partir da branch
  atual, nao da `master`;
- nao fazer merge para `master` sem decisao humana;
- nao fazer push automatico.

### 5. Implementation

O Executor aplica apenas as mudancas dentro do escopo. Qualquer alteracao fora
do escopo deve ser evitada; se for inevitavel, deve ser registrada no report.

### 6. Validation

O Executor roda os comandos definidos para a fase. Para fases Java, o checklist
padrao inclui:

```bash
./gradlew test
./gradlew build
git diff --check
```

Falhas ou checks nao executados devem ser registrados com detalhe.

### 7. Report

O Executor gera report factual em `harness/reports/`. Reports temporarios sao
ignorados pelo Git por padrao. Templates e READMEs continuam versionados.

O report deve registrar arquivos alterados, validacoes, resultados, riscos,
pendencias, fora de escopo e proxima etapa sugerida.

### 8. Review

O Architect e o Human Reviewer avaliam escopo, diff, validacoes e report. A
fase so deve ser considerada completa depois dessa revisao.

### 9. Next-step decision

O Human Reviewer decide se aprova, pede ajustes, faz push, faz merge ou inicia
a proxima fase. A decisao de merge para `master` nunca e automatica.

## Politica de reports

- Reports temporarios de Codex devem ficar em `harness/reports/`.
- `harness/reports/*.md` e ignorado pelo Git, exceto `README.md`.
- `harness/reports/.gitkeep` preserva o diretorio.
- Reports de fase podem ser versionados somente por decisao explicita.
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
