# Prompts

Prompts grandes ou reutilizaveis podem ser salvos como texto puro para reduzir
perda de contexto entre ChatGPT Web, Codex CLI e revisao humana.

Um prompt de execucao deve conter:

- contexto do projeto;
- fase ou microfase;
- objetivo;
- escopo;
- fora de escopo;
- branch strategy;
- arquivos ou areas esperadas;
- validacao obrigatoria;
- report esperado;
- criterios de completion.

Evite prompts sem branch strategy. Prompts com codigo devem ser mantidos como
texto simples, sem depender de formatacao proprietaria do chat.
