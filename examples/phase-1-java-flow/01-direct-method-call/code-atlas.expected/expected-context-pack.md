# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.direct.OrderController.create`
- Node count: 5
- Edge count: 5
- Resolution count: 0
- Boundary count: 0
- Unresolved count: 0

## Nodes

- `class:com.example.direct.OrderController` CLASS com.example.direct.OrderController
- `method:com.example.direct.OrderController.create` METHOD com.example.direct.OrderController.create
- `class:com.example.direct.OrderService` CLASS com.example.direct.OrderService
- `method:com.example.direct.OrderService.createOrder` METHOD com.example.direct.OrderService.createOrder
- `method:com.example.direct.OrderService.validate` METHOD com.example.direct.OrderService.validate

## Edges

- `declares:class:com.example.direct.OrderController->method:com.example.direct.OrderController.create` class:com.example.direct.OrderController -> method:com.example.direct.OrderController.create (DECLARES)
- `declares:class:com.example.direct.OrderService->method:com.example.direct.OrderService.createOrder` class:com.example.direct.OrderService -> method:com.example.direct.OrderService.createOrder (DECLARES)
- `calls:method:com.example.direct.OrderController.create->method:com.example.direct.OrderService.createOrder:11:1` method:com.example.direct.OrderController.create -> method:com.example.direct.OrderService.createOrder (CALLS)
- `declares:class:com.example.direct.OrderService->method:com.example.direct.OrderService.validate` class:com.example.direct.OrderService -> method:com.example.direct.OrderService.validate (DECLARES)
- `calls:method:com.example.direct.OrderService.createOrder->method:com.example.direct.OrderService.validate:5:1` method:com.example.direct.OrderService.createOrder -> method:com.example.direct.OrderService.validate (CALLS)

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

No unresolved symbols.

## AI Interpretations

None. This artifact contains deterministic facts only.
