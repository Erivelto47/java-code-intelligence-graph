# Blueprints

Blueprints registram a intencao tecnica antes da execucao. No harness
blueprint-driven, eles sao os primary inputs e a fonte de verdade de cada fase.

Cada fase deve comecar por um blueprint aprovado pelo Human Reviewer antes de
qualquer implementacao. O blueprint deve ser estavel o suficiente para derivar
handoff, validation checklist e completion criteria sem depender de memoria de
chat ou de interpretacoes paralelas.

Um blueprint deve explicar objetivo, motivacao, escopo, fora de escopo,
expected outputs, arquivos esperados, validacoes, report esperado, riscos e
criterios de sucesso.

Blueprints ajudam a evitar prompts soltos sem historico operacional. Eles podem
ser versionados quando representam uma fase ou decisao estavel. Rascunhos
temporarios devem ser tratados com cuidado para evitar ruido no repositorio.

## Conteudo recomendado

- fase ou microfase;
- objetivo principal;
- contexto relevante;
- escopo;
- fora de escopo;
- riscos conhecidos;
- expected outputs;
- branch strategy;
- arquivos ou areas esperadas;
- validacoes obrigatorias;
- report esperado;
- criterios de aceite;
- criterios de sucesso.

## Relacao com derivados

Os artefatos abaixo devem ser derivados do blueprint e nao devem divergir dele:

- `harness/handoffs/<phase>.handoff.md`
- `harness/validations/<phase>.validation.md`
- `harness/completion/<phase>.completion.md`

Se houver conflito entre um derivado e o blueprint, o blueprint vence e o
derivado deve ser corrigido antes da execucao.

## Runner

O blueprint runner consome blueprints aprovados com o formato:

```text
harness/blueprints/<phase>.blueprint.md
```

Comando:

```bash
./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

Ele deriva o phase id, cria derivados ausentes a partir dos templates e calcula
o runtime report path em `harness/reports/runs/`. O runner nao substitui a
aprovacao humana do blueprint nem executa a fase de produto.

## Pacotes versionados

- `phase-4-2-java-decision-unresolved-early-return.blueprint.md`: blueprint da
  Fase 4.2 para Decision Trace Java unresolved + early return.
