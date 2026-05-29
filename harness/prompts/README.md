# Prompts

Prompts grandes ou reutilizaveis podem ser salvos como texto puro para reduzir
perda de contexto entre ChatGPT Web, Codex CLI e revisao humana.

Um prompt de execucao deve conter:

- contexto do projeto;
- fase ou microfase;
- objetivo;
- roles envolvidas;
- escopo;
- fora de escopo;
- branch strategy;
- arquivos ou areas esperadas;
- validacao obrigatoria;
- report esperado;
- criterios de completion.

Evite prompts sem branch strategy. Prompts com codigo devem ser mantidos como
texto simples, sem depender de formatacao proprietaria do chat.

## Versioned templates and generated prompts

Reusable prompt templates live in `harness/prompts/` and are versioned.

The next phase runner renders:

```text
harness/prompts/codex-execution-prompt.template.txt
```

to:

```text
harness/bin/build/prompts/<phase-id>.codex-prompt.txt
```

Generated prompts under `harness/bin/build/prompts/` are temporary outputs and
are not versioned. They are intended to be reviewed and pasted into Codex by a
human-initiated execution session.
