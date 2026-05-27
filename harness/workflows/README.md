# Workflows

Workflows documentam como uma fase deve rodar dentro do harness.

Eles sao documentacao operacional, nao scripts. Um workflow registra sequencia,
papeis, entradas, saidas, validacao, report e revisao para que uma execucao
seja repetivel e auditavel.

Workflows devem permanecer simples, humanos e independentes de uma ferramenta
unica. Quando citarem ferramentas atuais, devem deixar claro a role:
ChatGPT Web Architect, Codex CLI Executor e Human Reviewer.

## Workflows iniciais

- `phase-execution.workflow.md`: ciclo minimo de intake, blueprint, prompt
  handoff, branch check, implementacao, validacao, report, review e decisao de
  proxima etapa.
