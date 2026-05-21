# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.controllerservice.CustomerController.register`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.controllerservice.CustomerController | CLASS | com.example.controllerservice.CustomerController | CustomerController |
| method:com.example.controllerservice.CustomerController.register | METHOD | com.example.controllerservice.CustomerController.register | register |
| class:com.example.controllerservice.CustomerService | CLASS | com.example.controllerservice.CustomerService | CustomerService |
| method:com.example.controllerservice.CustomerService.register | METHOD | com.example.controllerservice.CustomerService.register | register |
| method:com.example.controllerservice.CustomerService.normalize | METHOD | com.example.controllerservice.CustomerService.normalize | normalize |
| method:com.example.controllerservice.CustomerService.persist | METHOD | com.example.controllerservice.CustomerService.persist | persist |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.controllerservice.CustomerController->method:com.example.controllerservice.CustomerController.register | DECLARES | class:com.example.controllerservice.CustomerController | method:com.example.controllerservice.CustomerController.register |
| declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.register | DECLARES | class:com.example.controllerservice.CustomerService | method:com.example.controllerservice.CustomerService.register |
| calls:method:com.example.controllerservice.CustomerController.register->method:com.example.controllerservice.CustomerService.register:11:1 | CALLS | method:com.example.controllerservice.CustomerController.register | method:com.example.controllerservice.CustomerService.register |
| declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.normalize | DECLARES | class:com.example.controllerservice.CustomerService | method:com.example.controllerservice.CustomerService.normalize |
| calls:method:com.example.controllerservice.CustomerService.register->method:com.example.controllerservice.CustomerService.normalize:5:1 | CALLS | method:com.example.controllerservice.CustomerService.register | method:com.example.controllerservice.CustomerService.normalize |
| declares:class:com.example.controllerservice.CustomerService->method:com.example.controllerservice.CustomerService.persist | DECLARES | class:com.example.controllerservice.CustomerService | method:com.example.controllerservice.CustomerService.persist |
| calls:method:com.example.controllerservice.CustomerService.register->method:com.example.controllerservice.CustomerService.persist:6:2 | CALLS | method:com.example.controllerservice.CustomerService.register | method:com.example.controllerservice.CustomerService.persist |

## Resolutions

No inferred resolutions.

## Boundaries

No boundaries.

## Unresolved

No unresolved symbols.
