# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.interfaces.multiple.NotificationController.send`
- Node count: 8
- Edge count: 7
- Resolution count: 0
- Boundary count: 0
- Unresolved count: 1

## Nodes

- `class:com.example.interfaces.multiple.NotificationController` CLASS com.example.interfaces.multiple.NotificationController
- `method:com.example.interfaces.multiple.NotificationController.send` METHOD com.example.interfaces.multiple.NotificationController.send
- `interface:com.example.interfaces.multiple.NotificationSender` INTERFACE com.example.interfaces.multiple.NotificationSender
- `method:com.example.interfaces.multiple.NotificationSender.send` METHOD com.example.interfaces.multiple.NotificationSender.send
- `class:com.example.interfaces.multiple.EmailNotificationSender` CLASS com.example.interfaces.multiple.EmailNotificationSender
- `method:com.example.interfaces.multiple.EmailNotificationSender.send` METHOD com.example.interfaces.multiple.EmailNotificationSender.send
- `class:com.example.interfaces.multiple.SmsNotificationSender` CLASS com.example.interfaces.multiple.SmsNotificationSender
- `method:com.example.interfaces.multiple.SmsNotificationSender.send` METHOD com.example.interfaces.multiple.SmsNotificationSender.send

## Edges

- `declares:class:com.example.interfaces.multiple.NotificationController->method:com.example.interfaces.multiple.NotificationController.send` class:com.example.interfaces.multiple.NotificationController -> method:com.example.interfaces.multiple.NotificationController.send (DECLARES)
- `declares:interface:com.example.interfaces.multiple.NotificationSender->method:com.example.interfaces.multiple.NotificationSender.send` interface:com.example.interfaces.multiple.NotificationSender -> method:com.example.interfaces.multiple.NotificationSender.send (DECLARES)
- `calls:method:com.example.interfaces.multiple.NotificationController.send->method:com.example.interfaces.multiple.NotificationSender.send:11:1` method:com.example.interfaces.multiple.NotificationController.send -> method:com.example.interfaces.multiple.NotificationSender.send (CALLS)
- `implements:class:com.example.interfaces.multiple.EmailNotificationSender->interface:com.example.interfaces.multiple.NotificationSender` class:com.example.interfaces.multiple.EmailNotificationSender -> interface:com.example.interfaces.multiple.NotificationSender (IMPLEMENTS)
- `declares:class:com.example.interfaces.multiple.EmailNotificationSender->method:com.example.interfaces.multiple.EmailNotificationSender.send` class:com.example.interfaces.multiple.EmailNotificationSender -> method:com.example.interfaces.multiple.EmailNotificationSender.send (DECLARES)
- `implements:class:com.example.interfaces.multiple.SmsNotificationSender->interface:com.example.interfaces.multiple.NotificationSender` class:com.example.interfaces.multiple.SmsNotificationSender -> interface:com.example.interfaces.multiple.NotificationSender (IMPLEMENTS)
- `declares:class:com.example.interfaces.multiple.SmsNotificationSender->method:com.example.interfaces.multiple.SmsNotificationSender.send` class:com.example.interfaces.multiple.SmsNotificationSender -> method:com.example.interfaces.multiple.SmsNotificationSender.send (DECLARES)

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

- `com.example.interfaces.multiple.NotificationSender.send` from=`method:com.example.interfaces.multiple.NotificationSender.send` reason=`MULTIPLE_IMPLEMENTATIONS` confidence=`HIGH` candidates=`com.example.interfaces.multiple.EmailNotificationSender.send, com.example.interfaces.multiple.SmsNotificationSender.send`

## AI Interpretations

None. This artifact contains deterministic facts only.
