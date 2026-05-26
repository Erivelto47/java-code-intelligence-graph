# Harness do Code Atlas

A pasta `.harness` e a camada operacional versionada do Code Atlas. Ela orienta a execucao agentic, preserva contexto entre etapas, organiza decisoes e torna o processo repetivel sem misturar protocolo operacional com codigo de producao.

## Principio de portabilidade

O harness e AI-tool agnostic, vendor-neutral e independente de uma IA CLI especifica. Ele e baseado em arquivos versionados, Markdown e contratos textuais.

Ferramentas como Codex CLI, Claude Code ou outro executor equivalente podem usar este harness, desde que sigam o mesmo contrato operacional. Da mesma forma, ChatGPT Web pode atuar como implementacao atual do papel Planner/Reviewer, mas nao e uma dependencia obrigatoria do projeto.

## Papeis principais

### Planner/Reviewer

Define visao e arquitetura, cria blueprints, revisa reports, aprova a conclusao de uma etapa e define a proxima etapa.

### Executor CLI

Le blueprints e workflows, aplica mudancas no repositorio, respeita o escopo definido, roda validacoes, gera report de execucao e registra limitacoes ou pendencias.

### Human Operator

Decide quando executar, entrega prompts ao Executor CLI, retorna reports ao Planner/Reviewer, revisa commits, controla branches e aprova ou rejeita mudancas.

## Fluxo operacional basico

1. Planner/Reviewer define o blueprint da etapa.
2. Human Operator executa o prompt no Executor CLI.
3. Executor CLI le o harness e aplica as mudancas.
4. Executor CLI roda validacoes.
5. Executor CLI gera report em `/Users/eriveltomuller/Documents/GitHub/reports`.
6. Human Operator retorna o report ao Planner/Reviewer.
7. Planner/Reviewer revisa o resultado e define a proxima etapa.

## Pastas

- `workflows/`: descreve como uma etapa deve rodar.
- `blueprints/`: registra a intencao arquitetural antes da execucao.
- `agents/`: descreve papeis especializados, nao vendors.
- `handoffs/`: preserva contexto entre etapas.
- `reports/`: registra resultados de execucao quando forem versionaveis.
- `logs/`: guarda rastros operacionais quando necessario.
- `state/`: guarda estado leve e versionavel do andamento do projeto.

## Regras iniciais

- Blueprints representam intencao antes da execucao.
- Reports representam fatos depois da execucao.
- Handoffs preservam contexto entre etapas.
- Workflows descrevem como uma etapa deve rodar.
- Agents descrevem papeis, nao vendors.
- State deve ser leve e versionavel.
- Logs devem ser usados com cuidado para evitar ruido.
- Codigo de producao nao deve ser alterado sem blueprint ou tarefa explicita.
- Mudancas fora de escopo devem ser registradas no report.

## Fora de escopo nesta etapa

Esta etapa nao cria workflows detalhados, agentes especializados, templates de report, handoffs formais, automacoes executaveis, scripts ou integracoes com ferramentas especificas.
