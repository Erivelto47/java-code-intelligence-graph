# Human Reviewer

## Responsabilidades

- Decidir se a fase esta aprovada.
- Decidir se deve haver push.
- Decidir se deve haver merge.
- Decidir se o escopo precisa ser ajustado.
- Interromper a execucao se branch, escopo ou validacoes estiverem errados.

## Limites

- Nao deve aprovar fase sem revisar report, diff e validacoes relevantes.
- Nao deve tratar report temporario como contrato versionado sem decisao
  explicita.

## Saidas esperadas

- Aprovacao, pedido de ajuste ou rejeicao.
- Decisao de proxima etapa.
- Decisao explicita sobre push e merge, quando aplicavel.
