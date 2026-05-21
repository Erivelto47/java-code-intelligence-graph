# 01-direct-method-call

## Objetivo

Validar uma chamada direta entre classes concretas e a continuidade do fluxo para metodo interno.

## Entrypoint

`com.example.direct.OrderController.create`

## Fluxo Esperado

`OrderController.create` chama `OrderService.createOrder`, que chama `OrderService.validate`.

## O Que o Analyzer Deve Validar

- Resolver o receiver field `orderService` para o tipo declarado `OrderService`.
- Continuar o fluxo dentro de `OrderService.createOrder`.
- Incluir a chamada privada `OrderService.validate`.

## Criterios de Aceite

- O grafo contem os metodos `OrderController.create`, `OrderService.createOrder` e `OrderService.validate`.
- Existe edge `CALLS` de `OrderController.create` para `OrderService.createOrder`.
- Existe edge `CALLS` de `OrderService.createOrder` para `OrderService.validate`.
- Nao ha itens em `unresolved`.
