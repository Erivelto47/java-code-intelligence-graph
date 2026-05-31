# Handoffs

Handoffs preservam contexto entre fases, conversas, roles, ferramentas ou
operadores. No harness blueprint-driven, handoffs sao derivados do blueprint da
fase.

Eles devem ser objetivos e suficientes para que o Codex CLI Executor continue a
execucao sem depender de memoria de chat. Um handoff nao substitui o blueprint
nem o report:

```text
Blueprint = fonte primaria de verdade da fase.
Report = registro factual de uma execucao concluida.
Handoff = contexto operacional derivado para continuidade.
```

Use `handoff.template.md` quando houver transferencia entre ChatGPT Web
Architect, Codex CLI Executor e Human Reviewer, ou quando uma fase terminar
deixando proxima acao clara.

Handoffs podem ser gerados pelo blueprint runner:

```bash
./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

Quando o handoff ja existe, o runner nao sobrescreve por padrao e imprime
`exists, left unchanged`. O conteudo gerado inclui referencia ao blueprint
primario, paths derivados e aviso de precedencia do blueprint.

Handoffs nao devem divergir do blueprint nem ampliar escopo. Se houver conflito,
o blueprint vence e o handoff deve ser corrigido antes da execucao. Handoffs
temporarios devem evitar ruido no repositorio. Handoffs versionados devem
representar contexto estavel ou decisao operacional relevante.

## Pacotes versionados

- `phase-4-2-java-decision-unresolved-early-return.handoff.md`: handoff
  derivado e revisavel da Fase 4.2 para o Codex CLI Executor.
