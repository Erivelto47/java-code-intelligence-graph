# 02-controller-service

## Objetivo

Validar o padrao Controller para Service concreto, com receiver field injetado por construtor.

## Entrypoint

`com.example.controllerservice.CustomerController.register`

## Fluxo Esperado

`CustomerController.register` chama `CustomerService.register`, que chama `CustomerService.normalize` e `CustomerService.persist`.

## O Que o Analyzer Deve Validar

- Resolver o field `customerService` pelo tipo declarado no construtor.
- Continuar DFS no service concreto.
- Detectar multiplas chamadas internas no mesmo metodo.

## Criterios de Aceite

- O grafo contem `CustomerController.register`, `CustomerService.register`, `CustomerService.normalize` e `CustomerService.persist`.
- Existe edge `CALLS` do controller para o service.
- Existem edges `CALLS` do service para `normalize` e `persist`.
- Nao ha itens em `unresolved`.
