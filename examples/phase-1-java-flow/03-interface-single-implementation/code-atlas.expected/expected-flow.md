# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.interfaces.single.RegistrationController.register`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.interfaces.single.RegistrationController | CLASS | com.example.interfaces.single.RegistrationController | RegistrationController |
| method:com.example.interfaces.single.RegistrationController.register | METHOD | com.example.interfaces.single.RegistrationController.register | register |
| interface:com.example.interfaces.single.RegistrationUseCase | INTERFACE | com.example.interfaces.single.RegistrationUseCase | RegistrationUseCase |
| method:com.example.interfaces.single.RegistrationUseCase.create | METHOD | com.example.interfaces.single.RegistrationUseCase.create | create |
| class:com.example.interfaces.single.RegistrationService | CLASS | com.example.interfaces.single.RegistrationService | RegistrationService |
| method:com.example.interfaces.single.RegistrationService.create | METHOD | com.example.interfaces.single.RegistrationService.create | create |
| method:com.example.interfaces.single.RegistrationService.validate | METHOD | com.example.interfaces.single.RegistrationService.validate | validate |
| interface:com.example.interfaces.single.UserRepository | INTERFACE | com.example.interfaces.single.UserRepository | UserRepository |
| boundary:com.example.interfaces.single.UserRepository.save | BOUNDARY | com.example.interfaces.single.UserRepository.save | save |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.interfaces.single.RegistrationController->method:com.example.interfaces.single.RegistrationController.register | DECLARES | class:com.example.interfaces.single.RegistrationController | method:com.example.interfaces.single.RegistrationController.register |
| declares:interface:com.example.interfaces.single.RegistrationUseCase->method:com.example.interfaces.single.RegistrationUseCase.create | DECLARES | interface:com.example.interfaces.single.RegistrationUseCase | method:com.example.interfaces.single.RegistrationUseCase.create |
| calls:method:com.example.interfaces.single.RegistrationController.register->method:com.example.interfaces.single.RegistrationUseCase.create:11:1 | CALLS | method:com.example.interfaces.single.RegistrationController.register | method:com.example.interfaces.single.RegistrationUseCase.create |
| implements:class:com.example.interfaces.single.RegistrationService->interface:com.example.interfaces.single.RegistrationUseCase | IMPLEMENTS | class:com.example.interfaces.single.RegistrationService | interface:com.example.interfaces.single.RegistrationUseCase |
| declares:class:com.example.interfaces.single.RegistrationService->method:com.example.interfaces.single.RegistrationService.create | DECLARES | class:com.example.interfaces.single.RegistrationService | method:com.example.interfaces.single.RegistrationService.create |
| resolves:method:com.example.interfaces.single.RegistrationUseCase.create->method:com.example.interfaces.single.RegistrationService.create | RESOLVES_TO | method:com.example.interfaces.single.RegistrationUseCase.create | method:com.example.interfaces.single.RegistrationService.create |
| declares:class:com.example.interfaces.single.RegistrationService->method:com.example.interfaces.single.RegistrationService.validate | DECLARES | class:com.example.interfaces.single.RegistrationService | method:com.example.interfaces.single.RegistrationService.validate |
| calls:method:com.example.interfaces.single.RegistrationService.create->method:com.example.interfaces.single.RegistrationService.validate:15:1 | CALLS | method:com.example.interfaces.single.RegistrationService.create | method:com.example.interfaces.single.RegistrationService.validate |
| declares:interface:com.example.interfaces.single.UserRepository->boundary:com.example.interfaces.single.UserRepository.save | DECLARES | interface:com.example.interfaces.single.UserRepository | boundary:com.example.interfaces.single.UserRepository.save |
| calls:method:com.example.interfaces.single.RegistrationService.create->boundary:com.example.interfaces.single.UserRepository.save:16:2 | CALLS | method:com.example.interfaces.single.RegistrationService.create | boundary:com.example.interfaces.single.UserRepository.save |

## Resolutions

| Kind | Source | Target | Evidence | Confidence |
| --- | --- | --- | --- | --- |
| INTERFACE_SINGLE_IMPLEMENTATION | method:com.example.interfaces.single.RegistrationUseCase.create | method:com.example.interfaces.single.RegistrationService.create | INFERRED | HIGH |

## Boundaries

| Symbol | Kind | From | Reason | Confidence |
| --- | --- | --- | --- | --- |
| com.example.interfaces.single.UserRepository.save | REPOSITORY | method:com.example.interfaces.single.RegistrationService.create | REPOSITORY_BOUNDARY | HIGH |

## Unresolved

No unresolved symbols.
