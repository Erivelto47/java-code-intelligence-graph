# Agentic Harness do Code Atlas

O `harness/` e o protocolo operacional versionado do Code Atlas para fases
guiadas por blueprint. Ele organiza como uma intencao tecnica vira plano,
execucao no repositorio, validacao, report e decisao humana de continuidade.

Este harness nao executa codigo por si so. Ele e uma estrutura simples de
Markdown e arquivos leves para preservar contexto entre ChatGPT Web, Codex CLI
ou qualquer ferramenta equivalente.

## Por que existe

O projeto evolui em fases com contratos, fixtures e artefatos deterministas.
Sem um protocolo operacional, cada fase depende de prompts soltos e memoria de
chat. O harness reduz esse risco registrando:

- blueprints antes da execucao;
- prompts e handoffs entre papeis;
- reports depois da execucao;
- estado atual e validacoes;
- criterios objetivos de completion;
- decisoes operacionais sobre branch, merge, reports e artefatos.

## Papeis

### ChatGPT Web Architect

Transforma a intencao do usuario em blueprint tecnico, define escopo e fora de
escopo, gera prompt de handoff para o executor, revisa reports e sugere a
proxima etapa.

### Codex CLI Executor

Executa no repositorio, aplica mudancas dentro do escopo, roda validacoes,
gera report factual, registra riscos e limitacoes, e nao altera `master`, faz
merge ou push sem instrucao explicita.

### Human Reviewer

Aprova ou rejeita a fase, decide push, merge e ajustes de escopo, e interrompe
a execucao se branch, escopo ou validacoes estiverem errados.

## Como executar uma fase

1. Registrar ou revisar o blueprint em `harness/blueprints/`.
2. Gerar o prompt de handoff com escopo, fora de escopo, branch strategy,
   validacoes e report esperado.
3. Verificar `git status` e `git branch --show-current` antes de alterar
   arquivos.
4. Executar a implementacao somente na branch de trabalho da fase.
5. Rodar as validacoes registradas no blueprint ou checklist.
6. Gerar report factual em `harness/reports/`.
7. Enviar o report para revisao humana ou para o Architect.
8. Decidir a proxima etapa somente depois da revisao.

O workflow completo fica em
`harness/workflows/phase-execution.workflow.md`.

## Politica de branches

- A branch deve ser verificada antes de qualquer alteracao.
- Durante o ciclo atual de Fase 4 / harness, nao voltar para `master` para
  criar branch nova.
- Se uma microfase precisar de isolamento, criar a branch a partir da branch de
  trabalho atual.
- Nao fazer merge para `master` sem decisao humana explicita.
- Nao fazer push automatico.

## Politica de reports

- Reports temporarios gerados por executor devem ficar em `harness/reports/`.
- `harness/reports/` existe no Git via `.gitkeep` e `README.md`.
- Reports temporarios `harness/reports/*.md` sao ignorados pelo Git, salvo
  decisao explicita de versionar um report especifico.
- Templates, READMEs e convencoes do harness permanecem versionados.
- Artefatos de fixture ou examples podem ser versionados quando fizerem parte
  de um contrato do produto.

## Conexao com o Code Atlas

O harness organiza a execucao do projeto, mas nao substitui os contratos do
produto. Decision Trace, Flow Graph, Project Index e outros artefatos continuam
pertencendo ao dominio do Code Atlas.

Em especial, `harness/decisions/` registra decisoes operacionais do processo.
Ele nao e equivalente a `.code-atlas/decisions/`, aos pacotes
`com.codeatlas.core.decision` ou aos artefatos de Decision Trace.

## O que o harness nao substitui

- testes automatizados;
- revisao humana;
- contratos de produto;
- validacao por build;
- controle consciente de branch, commit, push e merge.

## Estrutura

```text
harness/
  workflows/      Workflows operacionais versionados.
  agents/         Papeis e responsabilidades.
  blueprints/     Planos tecnicos antes da execucao.
  handoffs/       Transferencia de contexto entre papeis.
  reports/        Politica e reports temporarios ignorados.
  state/          Templates de estado de execucao.
  validations/    Checklists de validacao.
  completion/     Criterios de conclusao.
  prompts/        Convencoes para prompts reutilizaveis.
  decisions/      Decisoes operacionais do harness.
```
