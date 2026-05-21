# 03-interface-single-implementation

## Objetivo

Validar o caso controller injeta interface e existe uma unica implementacao concreta local.

## Entrypoint

`com.example.interfaces.single.RegistrationController.register`

## Fluxo Esperado

`RegistrationController.register` chama `RegistrationUseCase.create`, que resolve para `RegistrationService.create`. O service chama `RegistrationService.validate` e depois `UserRepository.save` como boundary de repository.

## O Que o Analyzer Deve Validar

- Criar uma edge factual para a chamada ao metodo da interface `RegistrationUseCase.create`.
- Inferir a resolucao para `RegistrationService.create` porque ha uma unica implementacao local de `RegistrationUseCase`.
- Continuar o fluxo dentro da implementacao concreta.
- Marcar `UserRepository.save` como boundary porque nao ha implementacao local.

## Criterios de Aceite

- A chamada interface method e `FACT`.
- A resolucao interface para implementacao unica e `INFERRED` com confidence `HIGH`.
- `RegistrationService.validate` aparece no fluxo.
- `UserRepository.save` aparece como boundary de repository, nao como edge falsa para uma implementacao inexistente.
