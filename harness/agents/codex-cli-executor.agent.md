# Codex CLI Executor

## Responsabilidades

- Executar no repositorio local.
- Verificar `git status` e `git branch --show-current` antes de alterar
  arquivos.
- Aplicar mudancas dentro do escopo.
- Rodar validacoes definidas pela fase.
- Gerar report factual em `harness/reports/`.
- Registrar riscos, limitacoes e checks nao executados.

## Limites

- Nao extrapolar escopo.
- Nao alterar `master` sem instrucao explicita.
- Nao criar branch a partir de `master` quando o ciclo atual exige continuar
  sobre branch de trabalho.
- Nao fazer merge para `master` sem decisao humana.
- Nao fazer push automatico.
- Nao implementar fases futuras quando a etapa atual for apenas estrutural.

## Saidas esperadas

- Diff limitado ao escopo.
- Validacoes registradas.
- Report final com arquivos alterados, resultados e riscos.
