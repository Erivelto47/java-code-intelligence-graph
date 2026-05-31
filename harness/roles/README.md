# Roles

Este diretorio descreve responsabilidades operacionais do harness. Uma role e
um papel dentro do workflow: ela pode ser executada por uma pessoa, por uma
ferramenta assistida por IA ou por uma combinacao dos dois.

O harness usa o termo "agentic workflow" para descrever o padrao de
colaboracao, mas documenta os participantes como roles. Agent e apenas uma
possivel implementacao automatizada de uma role; nao e o conceito principal
deste harness.

## Roles minimas

- `chatgpt-web-architect.role.md`: role de arquitetura assistida por IA,
  blueprint, handoff e revisao.
- `codex-cli-executor.role.md`: role de execucao com apoio de IA/ferramenta no
  repositorio local.
- `human-reviewer.role.md`: role humana para aprovacao, rejeicao, merge, push
  e ajuste de escopo.

## Distincao importante

Human Reviewer e uma role humana, nao um agente autonomo. Codex CLI Executor e
uma role operacional executada com apoio de IA e ferramenta local. ChatGPT Web
Architect e uma role de arquitetura assistida por IA.

Cada role deve respeitar o workflow de fase, a politica de branch, a politica
de reports e os limites de escopo registrados no harness.
