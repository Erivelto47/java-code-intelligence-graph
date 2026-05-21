# 06-external-client-boundary

## Objetivo

Validar chamada para client externo como boundary apos resolver uma implementacao unica local.

## Entrypoint

`com.example.externalclient.PaymentService.pay`

## Fluxo Esperado

`PaymentService.pay` chama `PaymentGatewayClient.authorize`, que resolve para `HttpPaymentGatewayClient.authorize`. A implementacao chama `HttpClient.post` como external boundary.

## O Que o Analyzer Deve Validar

- Resolver `paymentGatewayClient` para a interface declarada.
- Inferir `HttpPaymentGatewayClient.authorize` como unica implementacao local de `PaymentGatewayClient.authorize`.
- Marcar `HttpClient.post` como boundary externo porque nao ha implementacao local e o nome representa um client/framework externo.

## Criterios de Aceite

- A chamada para `PaymentGatewayClient.authorize` e factual.
- A resolucao para `HttpPaymentGatewayClient.authorize` e inferida com confidence `HIGH`.
- `HttpClient.post` aparece como boundary externa.
- Nao ha edge falsa alem de `HttpClient.post`.
