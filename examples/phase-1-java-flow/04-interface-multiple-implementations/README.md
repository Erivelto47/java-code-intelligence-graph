# 04-interface-multiple-implementations

## Objetivo

Validar que o analyzer nao inventa uma implementacao quando uma interface possui multiplas implementacoes locais.

## Entrypoint

`com.example.interfaces.multiple.NotificationController.send`

## Fluxo Esperado

`NotificationController.send` chama `NotificationSender.send`. A continuacao para `EmailNotificationSender.send` ou `SmsNotificationSender.send` deve ficar em `unresolved` com reason `MULTIPLE_IMPLEMENTATIONS`.

## O Que o Analyzer Deve Validar

- Detectar a chamada factual para o metodo da interface.
- Detectar candidatos locais que implementam a interface.
- Nao criar edge automatica para nenhuma implementacao concreta.
- Registrar candidatos em `unresolved`.

## Criterios de Aceite

- Existe edge `CALLS` de `NotificationController.send` para `NotificationSender.send`.
- Nao existe edge `RESOLVES_TO` para `EmailNotificationSender.send` nem para `SmsNotificationSender.send`.
- `unresolved` contem reason `MULTIPLE_IMPLEMENTATIONS` com os dois candidatos.
