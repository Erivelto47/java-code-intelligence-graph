# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.controllerservice.CustomerController.register`
- Node count: 6
- Edge count: 7
- Resolution count: 0
- Boundary count: 0
- Unresolved count: 0

## Nodes

- `class:com.example.controllerservice.CustomerController` CLASS com.example.controllerservice.CustomerController
- `method:com.example.controllerservice.CustomerController.register` METHOD com.example.controllerservice.CustomerController.register
- `class:com.example.controllerservice.CustomerService` CLASS com.example.controllerservice.CustomerService
- `method:com.example.controllerservice.CustomerService.register` METHOD com.example.controllerservice.CustomerService.register
- `method:com.example.controllerservice.CustomerService.normalize` METHOD com.example.controllerservice.CustomerService.normalize
- `method:com.example.controllerservice.CustomerService.persist` METHOD com.example.controllerservice.CustomerService.persist

## Edges

- `declares:class:com.example.controllerservice.CustomerController->method:com.example.controllerservice.CustomerController.register` class:com.example.controllerservice.CustomerController -> method:com.example.controllerservice.CustomerController.register (DECLARES)
- `declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.register` class:com.example.controllerservice.CustomerService -> method:com.example.controllerservice.CustomerService.register (DECLARES)
- `calls:method:com.example.controllerservice.CustomerController.register->method:com.example.controllerservice.CustomerService.register:11:1` method:com.example.controllerservice.CustomerController.register -> method:com.example.controllerservice.CustomerService.register (CALLS)
- `declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.normalize` class:com.example.controllerservice.CustomerService -> method:com.example.controllerservice.CustomerService.normalize (DECLARES)
- `calls:method:com.example.controllerservice.CustomerService.register->method:com.example.controllerservice.CustomerService.normalize:5:1` method:com.example.controllerservice.CustomerService.register -> method:com.example.controllerservice.CustomerService.normalize (CALLS)
- `declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.persist` class:com.example.controllerservice.CustomerService -> method:com.example.controllerservice.CustomerService.persist (DECLARES)
- `calls:method:com.example.controllerservice.CustomerService.register->method:com.example.controllerservice.CustomerService.persist:6:2` method:com.example.controllerservice.CustomerService.register -> method:com.example.controllerservice.CustomerService.persist (CALLS)

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

No unresolved symbols.

## AI Interpretations

None. This artifact contains deterministic facts only.
