# 05-repository-boundary

## Objetivo

Validar chamadas para repository como boundary quando a interface nao possui implementacao local.

## Entrypoint

`com.example.repository.AccountService.openAccount`

## Fluxo Esperado

`AccountService.openAccount` chama `AccountRepository.findByDocument` e `AccountRepository.save`, ambos como boundaries de repository.

## O Que o Analyzer Deve Validar

- Resolver o receiver field `accountRepository` para a interface declarada `AccountRepository`.
- Identificar ausencia de implementacao concreta local.
- Marcar chamadas de repository como boundary ou unresolved controlado.

## Criterios de Aceite

- As chamadas `findByDocument` e `save` aparecem como boundaries.
- Nao ha edge falsa para implementacao inexistente.
- Nao ha continuacao de fluxo alem da boundary.
