# Agents

Este diretorio descreve papeis operacionais do harness. Os papeis podem ser
executados por pessoas ou por ferramentas diferentes; os arquivos nao criam
agentes executaveis.

## Papeis minimos

- `chatgpt-web-architect.agent.md`: arquiteto, gerador de blueprint e revisor.
- `codex-cli-executor.agent.md`: executor no repositorio.
- `human-reviewer.agent.md`: decisor humano sobre aprovacao, push, merge e
  ajuste de escopo.

Cada papel deve respeitar o workflow de fase e a politica de branch/report
registrada no harness.
