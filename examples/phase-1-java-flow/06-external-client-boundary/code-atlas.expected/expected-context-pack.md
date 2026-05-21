# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.externalclient.PaymentService.pay`
- Node count: 8
- Edge count: 8
- Resolution count: 1
- Boundary count: 1
- Unresolved count: 0

## Nodes

- `class:com.example.externalclient.PaymentService` CLASS com.example.externalclient.PaymentService
- `method:com.example.externalclient.PaymentService.pay` METHOD com.example.externalclient.PaymentService.pay
- `interface:com.example.externalclient.PaymentGatewayClient` INTERFACE com.example.externalclient.PaymentGatewayClient
- `method:com.example.externalclient.PaymentGatewayClient.authorize` METHOD com.example.externalclient.PaymentGatewayClient.authorize
- `class:com.example.externalclient.HttpPaymentGatewayClient` CLASS com.example.externalclient.HttpPaymentGatewayClient
- `method:com.example.externalclient.HttpPaymentGatewayClient.authorize` METHOD com.example.externalclient.HttpPaymentGatewayClient.authorize
- `interface:com.example.externalclient.HttpClient` INTERFACE com.example.externalclient.HttpClient
- `boundary:com.example.externalclient.HttpClient.post` BOUNDARY com.example.externalclient.HttpClient.post

## Edges

- `declares:class:com.example.externalclient.PaymentService->method:com.example.externalclient.PaymentService.pay` class:com.example.externalclient.PaymentService -> method:com.example.externalclient.PaymentService.pay (DECLARES)
- `declares:interface:com.example.externalclient.PaymentGatewayClient->method:com.example.externalclient.PaymentGatewayClient.authorize` interface:com.example.externalclient.PaymentGatewayClient -> method:com.example.externalclient.PaymentGatewayClient.authorize (DECLARES)
- `calls:method:com.example.externalclient.PaymentService.pay->method:com.example.externalclient.PaymentGatewayClient.authorize:11:1` method:com.example.externalclient.PaymentService.pay -> method:com.example.externalclient.PaymentGatewayClient.authorize (CALLS)
- `implements:class:com.example.externalclient.HttpPaymentGatewayClient->interface:com.example.externalclient.PaymentGatewayClient` class:com.example.externalclient.HttpPaymentGatewayClient -> interface:com.example.externalclient.PaymentGatewayClient (IMPLEMENTS)
- `declares:class:com.example.externalclient.HttpPaymentGatewayClient->method:com.example.externalclient.HttpPaymentGatewayClient.authorize` class:com.example.externalclient.HttpPaymentGatewayClient -> method:com.example.externalclient.HttpPaymentGatewayClient.authorize (DECLARES)
- `resolves:method:com.example.externalclient.PaymentGatewayClient.authorize->method:com.example.externalclient.HttpPaymentGatewayClient.authorize` method:com.example.externalclient.PaymentGatewayClient.authorize -> method:com.example.externalclient.HttpPaymentGatewayClient.authorize (RESOLVES_TO)
- `declares:interface:com.example.externalclient.HttpClient->boundary:com.example.externalclient.HttpClient.post` interface:com.example.externalclient.HttpClient -> boundary:com.example.externalclient.HttpClient.post (DECLARES)
- `calls:method:com.example.externalclient.HttpPaymentGatewayClient.authorize->boundary:com.example.externalclient.HttpClient.post:12:1` method:com.example.externalclient.HttpPaymentGatewayClient.authorize -> boundary:com.example.externalclient.HttpClient.post (CALLS)

## Resolutions

- `INTERFACE_SINGLE_IMPLEMENTATION` `method:com.example.externalclient.PaymentGatewayClient.authorize` -> `method:com.example.externalclient.HttpPaymentGatewayClient.authorize` evidence=`INFERRED` confidence=`HIGH`

## Boundaries

- `com.example.externalclient.HttpClient.post` kind=`HTTP_CLIENT` from=`method:com.example.externalclient.HttpPaymentGatewayClient.authorize` reason=`NO_LOCAL_IMPLEMENTATION` confidence=`HIGH`

## Unresolved

No unresolved symbols.

## AI Interpretations

None. This artifact contains deterministic facts only.
