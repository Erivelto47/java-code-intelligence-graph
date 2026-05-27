# Reports

Reports sao registros factuais produzidos depois de uma tarefa ou fase.

Eles capturam o que o Executor realmente fez: arquivos criados, alterados ou
removidos, validacoes executadas, testes, falhas, riscos, pendencias e proxima
etapa sugerida.

## Politica inicial

- Reports temporarios gerados por Codex devem ficar em `harness/reports/`.
- Arquivos `harness/reports/*.md` sao ignorados pelo Git por padrao.
- `README.md` e `.gitkeep` permanecem versionados.
- Um report temporario so deve ser versionado por decisao explicita.
- Reports nao devem inventar sucesso: falhas, limitacoes e checks pulados
  devem ser registrados.

## Nome sugerido

Use nomes estaveis e especificos:

```text
HARNESS_0_1_AGENTIC_WORKFLOW_SKELETON_REPORT.md
PHASE_4_2_DECISION_UNRESOLVED_REPORT.md
```

## Conteudo minimo

- resumo;
- motivacao;
- branch strategy usada;
- o que mudou;
- validacoes executadas;
- resultados;
- arquivos alterados;
- confirmacao de fora de escopo;
- riscos conhecidos;
- proxima fase sugerida.
