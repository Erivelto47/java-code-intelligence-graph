# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.interfaces.multiple.NotificationController.send`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.interfaces.multiple.NotificationController | CLASS | com.example.interfaces.multiple.NotificationController | NotificationController |
| method:com.example.interfaces.multiple.NotificationController.send | METHOD | com.example.interfaces.multiple.NotificationController.send | send |
| interface:com.example.interfaces.multiple.NotificationSender | INTERFACE | com.example.interfaces.multiple.NotificationSender | NotificationSender |
| method:com.example.interfaces.multiple.NotificationSender.send | METHOD | com.example.interfaces.multiple.NotificationSender.send | send |
| class:com.example.interfaces.multiple.EmailNotificationSender | CLASS | com.example.interfaces.multiple.EmailNotificationSender | EmailNotificationSender |
| method:com.example.interfaces.multiple.EmailNotificationSender.send | METHOD | com.example.interfaces.multiple.EmailNotificationSender.send | send |
| class:com.example.interfaces.multiple.SmsNotificationSender | CLASS | com.example.interfaces.multiple.SmsNotificationSender | SmsNotificationSender |
| method:com.example.interfaces.multiple.SmsNotificationSender.send | METHOD | com.example.interfaces.multiple.SmsNotificationSender.send | send |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.interfaces.multiple.NotificationController->method:com.example.interfaces.multiple.NotificationController.send | DECLARES | class:com.example.interfaces.multiple.NotificationController | method:com.example.interfaces.multiple.NotificationController.send |
| declares:interface:com.example.interfaces.multiple.NotificationSender->method:com.example.interfaces.multiple.NotificationSender.send | DECLARES | interface:com.example.interfaces.multiple.NotificationSender | method:com.example.interfaces.multiple.NotificationSender.send |
| calls:method:com.example.interfaces.multiple.NotificationController.send->method:com.example.interfaces.multiple.NotificationSender.send:11:1 | CALLS | method:com.example.interfaces.multiple.NotificationController.send | method:com.example.interfaces.multiple.NotificationSender.send |
| implements:class:com.example.interfaces.multiple.EmailNotificationSender->interface:com.example.interfaces.multiple.NotificationSender | IMPLEMENTS | class:com.example.interfaces.multiple.EmailNotificationSender | interface:com.example.interfaces.multiple.NotificationSender |
| declares:class:com.example.interfaces.multiple.EmailNotificationSender->method:com.example.interfaces.multiple.EmailNotificationSender.send | DECLARES | class:com.example.interfaces.multiple.EmailNotificationSender | method:com.example.interfaces.multiple.EmailNotificationSender.send |
| implements:class:com.example.interfaces.multiple.SmsNotificationSender->interface:com.example.interfaces.multiple.NotificationSender | IMPLEMENTS | class:com.example.interfaces.multiple.SmsNotificationSender | interface:com.example.interfaces.multiple.NotificationSender |
| declares:class:com.example.interfaces.multiple.SmsNotificationSender->method:com.example.interfaces.multiple.SmsNotificationSender.send | DECLARES | class:com.example.interfaces.multiple.SmsNotificationSender | method:com.example.interfaces.multiple.SmsNotificationSender.send |

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

| Symbol | From | Reason | Confidence | Candidates |
| --- | --- | --- | --- | --- |
| com.example.interfaces.multiple.NotificationSender.send | method:com.example.interfaces.multiple.NotificationSender.send | MULTIPLE_IMPLEMENTATIONS | HIGH | com.example.interfaces.multiple.EmailNotificationSender.send, com.example.interfaces.multiple.SmsNotificationSender.send |
