# Human Reviewer

## Role type

Human decision maker

## Purpose

Exercer decisao humana sobre aprovacao, rejeicao, riscos aceitos, push, merge e
proxima etapa. Esta e uma role humana, nao um agente automatizado.

## Responsibilities

- Revisar report, diff e validacoes relevantes.
- Decidir se a fase esta aprovada.
- Decidir se deve haver push.
- Decidir se deve haver merge.
- Decidir se o escopo precisa ser ajustado.
- Interromper a execucao se branch, escopo ou validacoes estiverem errados.
- Decidir a proxima etapa.
- Aceitar ou rejeitar riscos conhecidos.

## Inputs

- Report factual do Executor.
- Diff, status da branch e resultados de validacao.
- Revisao ou recomendacao do ChatGPT Web Architect, quando houver.
- Riscos conhecidos e perguntas abertas.

## Outputs

- Aprovacao, pedido de ajuste ou rejeicao.
- Decisao explicita sobre push, merge e continuidade.
- Aceite ou rejeicao de riscos conhecidos.
- Direcao para a proxima fase ou microfase.

## Boundaries

- Nao deve aprovar fase sem revisar report, diff e validacoes relevantes.
- Nao deve tratar report temporario como contrato versionado sem decisao
  explicita.
- Nao deve autorizar merge para `master` se a consolidacao da branch de
  trabalho ainda nao foi aprovada.

## Must not do

- Ser descrito como agente autonomo.
- Delegar aprovacao final para uma ferramenta.
- Aprovar push ou merge de forma implicita.
- Ignorar desvios de branch, escopo ou validacao.

## Handoff expectations

Quando pedir continuidade, declarar objetivo, escopo, branch strategy,
validacoes esperadas, restricoes e se push ou merge permanecem bloqueados.

## Validation expectations

Confirmar se os comandos obrigatorios foram executados e se seus resultados
sustentam a aprovacao. Falhas conhecidas podem ser aceitas apenas com decisao
humana explicita.
