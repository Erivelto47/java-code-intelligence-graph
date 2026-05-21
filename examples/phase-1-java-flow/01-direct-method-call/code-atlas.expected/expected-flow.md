# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.direct.OrderController.create`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.direct.OrderController | CLASS | com.example.direct.OrderController | OrderController |
| method:com.example.direct.OrderController.create | METHOD | com.example.direct.OrderController.create | create |
| class:com.example.direct.OrderService | CLASS | com.example.direct.OrderService | OrderService |
| method:com.example.direct.OrderService.createOrder | METHOD | com.example.direct.OrderService.createOrder | createOrder |
| method:com.example.direct.OrderService.validate | METHOD | com.example.direct.OrderService.validate | validate |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.direct.OrderController->method:com.example.direct.OrderController.create | DECLARES | class:com.example.direct.OrderController | method:com.example.direct.OrderController.create |
| declares:class:com.example.direct.OrderService->method:com.example.direct.OrderService.createOrder | DECLARES | class:com.example.direct.OrderService | method:com.example.direct.OrderService.createOrder |
| calls:method:com.example.direct.OrderController.create->method:com.example.direct.OrderService.createOrder:11:1 | CALLS | method:com.example.direct.OrderController.create | method:com.example.direct.OrderService.createOrder |
| declares:class:com.example.direct.OrderService->method:com.example.direct.OrderService.validate | DECLARES | class:com.example.direct.OrderService | method:com.example.direct.OrderService.validate |
| calls:method:com.example.direct.OrderService.createOrder->method:com.example.direct.OrderService.validate:5:1 | CALLS | method:com.example.direct.OrderService.createOrder | method:com.example.direct.OrderService.validate |

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

No unresolved symbols.
