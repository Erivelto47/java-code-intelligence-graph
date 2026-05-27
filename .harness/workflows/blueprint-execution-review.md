# Workflow — Blueprint Execution Review

## Objetivo

Este workflow padroniza o ciclo de execucao de uma etapa do Code Atlas guiada
por blueprint, desde a decisao inicial ate a revisao do resultado e a definicao
da proxima etapa.

## Papéis

- Planner/Reviewer: define o blueprint, revisa o resultado, aprova ajustes e
  decide a proxima etapa.
- Executor CLI: le o harness, executa mudancas dentro do escopo, valida o que
  foi feito e registra o resultado.
- Human Operator: entrega prompts ao Executor CLI, retorna reports ao
  Planner/Reviewer e controla commits, branches e aprovacao final.

## Entradas

- Blueprint ou decisao de etapa.
- Prompt de execucao.
- Estado atual do repositorio.
- Harness atual.
- Restricoes de escopo.
- Diretorio local temporario de reports:
  `.harness/reports/runs/`.

## Saídas

- Alteracoes no repositorio.
- Validacoes executadas.
- Report local temporario ignorado pelo Git.
- Possiveis handoffs.
- Proxima etapa sugerida.

## Ciclo operacional

1. Define blueprint
   O Planner/Reviewer define a intencao, os limites e o criterio de conclusao
   da etapa.
2. Prepare execution prompt
   O Human Operator prepara ou entrega o prompt com o blueprint e as restricoes
   relevantes.
3. Run Executor CLI
   O Human Operator executa o prompt em um Executor CLI compativel com o
   harness.
4. Apply scoped changes
   O Executor CLI le o harness, aplica apenas as mudancas permitidas e registra
   qualquer desvio necessario.
5. Validate
   O Executor CLI roda as validacoes adequadas ao escopo e registra resultados,
   falhas ou checks nao executados.
6. Generate report
   O Executor CLI gera um report factual em `.harness/reports/runs/`, usando
   caminho relativo ao root do repositorio. Esse diretorio e ignorado pelo Git.
7. Return report to Planner/Reviewer
   O Human Operator retorna o report ao Planner/Reviewer para revisao.
8. Review outcome
   O Planner/Reviewer avalia as mudancas, validacoes, riscos e pendencias.
9. Decide next step
   O Planner/Reviewer aprova, solicita correcao ou define a proxima etapa.

## Regras

- O Executor CLI deve respeitar o escopo do blueprint.
- Mudancas fora de escopo devem ser registradas no report.
- Reports reais ou temporarios devem ser gerados em `.harness/reports/runs/`.
- Reports em `.harness/reports/runs/` sao outputs locais e ignorados pelo Git.
- Templates e convencoes de reports permanecem versionados em
  `.harness/reports/`.
- Handoffs devem ser usados quando houver transferencia relevante de contexto.
- Falhas devem ser registradas explicitamente.
- O workflow nao exige vendor especifico.
- Codigo de producao so deve ser alterado quando o blueprint permitir.
- O Human Operator controla commits, branches e aprovacao final, salvo instrucao
  explicita diferente.

## Quando usar report

Use report ao concluir uma etapa, tarefa ou execucao relevante. O report deve
registrar fatos observados: arquivos criados, arquivos alterados, arquivos
removidos, validacoes, testes, decisoes, riscos, pendencias e proxima etapa
sugerida.

Reports reais ou temporarios devem ser gerados em `.harness/reports/runs/`,
como outputs locais ignorados pelo Git.

## Quando usar handoff

Use handoff quando houver transferencia relevante de contexto entre etapas,
sessoes, operadores ou executores. O handoff deve resumir o estado atual,
decisoes importantes, riscos conhecidos, pendencias e a acao recomendada para a
continuidade.

Um handoff nao substitui o report da etapa concluida.

## Critérios de conclusão

- Arquivos esperados foram criados ou alterados.
- Validacoes executadas foram registradas.
- Report local temporario foi gerado em `.harness/reports/runs/`.
- Riscos ou pendencias foram registrados.
- Planner/Reviewer consegue decidir a proxima etapa.

## Fora de escopo

Este workflow nao cria:

- Scripts.
- Automacoes executaveis.
- Integracao com vendors.
- Politica real de permissao ou sandbox.
- Agentes especializados.
