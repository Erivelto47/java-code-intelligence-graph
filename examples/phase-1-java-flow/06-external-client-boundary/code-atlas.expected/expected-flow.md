# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.externalclient.PaymentService.pay`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.externalclient.PaymentService | CLASS | com.example.externalclient.PaymentService | PaymentService |
| method:com.example.externalclient.PaymentService.pay | METHOD | com.example.externalclient.PaymentService.pay | pay |
| interface:com.example.externalclient.PaymentGatewayClient | INTERFACE | com.example.externalclient.PaymentGatewayClient | PaymentGatewayClient |
| method:com.example.externalclient.PaymentGatewayClient.authorize | METHOD | com.example.externalclient.PaymentGatewayClient.authorize | authorize |
| class:com.example.externalclient.HttpPaymentGatewayClient | CLASS | com.example.externalclient.HttpPaymentGatewayClient | HttpPaymentGatewayClient |
| method:com.example.externalclient.HttpPaymentGatewayClient.authorize | METHOD | com.example.externalclient.HttpPaymentGatewayClient.authorize | authorize |
| interface:com.example.externalclient.HttpClient | INTERFACE | com.example.externalclient.HttpClient | HttpClient |
| boundary:com.example.externalclient.HttpClient.post | BOUNDARY | com.example.externalclient.HttpClient.post | post |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.externalclient.PaymentService->method:com.example.externalclient.PaymentService.pay | DECLARES | class:com.example.externalclient.PaymentService | method:com.example.externalclient.PaymentService.pay |
| declares:interface:com.example.externalclient.PaymentGatewayClient->method:com.example.externalclient.PaymentGatewayClient.authorize | DECLARES | interface:com.example.externalclient.PaymentGatewayClient | method:com.example.externalclient.PaymentGatewayClient.authorize |
| calls:method:com.example.externalclient.PaymentService.pay->method:com.example.externalclient.PaymentGatewayClient.authorize:11:1 | CALLS | method:com.example.externalclient.PaymentService.pay | method:com.example.externalclient.PaymentGatewayClient.authorize |
| implements:class:com.example.externalclient.HttpPaymentGatewayClient->interface:com.example.externalclient.PaymentGatewayClient | IMPLEMENTS | class:com.example.externalclient.HttpPaymentGatewayClient | interface:com.example.externalclient.PaymentGatewayClient |
| declares:class:com.example.externalclient.HttpPaymentGatewayClient->method:com.example.externalclient.HttpPaymentGatewayClient.authorize | DECLARES | class:com.example.externalclient.HttpPaymentGatewayClient | method:com.example.externalclient.HttpPaymentGatewayClient.authorize |
| resolves:method:com.example.externalclient.PaymentGatewayClient.authorize->method:com.example.externalclient.HttpPaymentGatewayClient.authorize | RESOLVES_TO | method:com.example.externalclient.PaymentGatewayClient.authorize | method:com.example.externalclient.HttpPaymentGatewayClient.authorize |
| declares:interface:com.example.externalclient.HttpClient->boundary:com.example.externalclient.HttpClient.post | DECLARES | interface:com.example.externalclient.HttpClient | boundary:com.example.externalclient.HttpClient.post |
| calls:method:com.example.externalclient.HttpPaymentGatewayClient.authorize->boundary:com.example.externalclient.HttpClient.post:12:1 | CALLS | method:com.example.externalclient.HttpPaymentGatewayClient.authorize | boundary:com.example.externalclient.HttpClient.post |

## Resolutions

| Kind | Source | Target | Evidence | Confidence |
| --- | --- | --- | --- | --- |
| INTERFACE_SINGLE_IMPLEMENTATION | method:com.example.externalclient.PaymentGatewayClient.authorize | method:com.example.externalclient.HttpPaymentGatewayClient.authorize | INFERRED | HIGH |

## Boundaries

| Symbol | Kind | From | Reason | Confidence |
| --- | --- | --- | --- | --- |
| com.example.externalclient.HttpClient.post | HTTP_CLIENT | method:com.example.externalclient.HttpPaymentGatewayClient.authorize | NO_LOCAL_IMPLEMENTATION | HIGH |

## Unresolved

No unresolved symbols.
