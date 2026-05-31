# Reports

Reports sao registros factuais produzidos depois de uma tarefa ou fase.

Eles capturam o que o Executor realmente fez: arquivos criados, alterados ou
removidos, validacoes executadas, testes, falhas, riscos, pendencias e proxima
etapa sugerida.

## Politica

- Reports temporarios reais gerados por Codex devem ficar em
  `harness/reports/runs/`.
- Root-level `harness/reports/` e reservado para politica de reports,
  marcadores de diretorio e templates versionados.
- Temporary real reports must be generated under `harness/reports/runs/`.
- Root-level `harness/reports/` is reserved for report policy, directory
  markers and versioned templates.
- `README.md`, `.gitkeep` e templates podem permanecer versionados.
- Templates em `harness/reports/templates/` podem ser versionados.
- `harness/reports/runs/` e ignorado pelo Git; o diretorio pode ser criado sob
  demanda por uma execucao.
- Um report temporario so deve ser versionado por decisao explicita.
- Reports anexados ao chat podem ser usados para revisao, mas nao sao
  necessariamente artefatos permanentes do repositorio.
- Reports nao devem inventar sucesso: falhas, limitacoes e checks pulados
  devem ser registrados.

## Nome sugerido

Use nomes estaveis e especificos:

```text
harness/reports/runs/HARNESS_0_1_AGENTIC_WORKFLOW_SKELETON_REPORT.md
harness/reports/runs/PHASE_4_2_DECISION_UNRESOLVED_REPORT.md
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
