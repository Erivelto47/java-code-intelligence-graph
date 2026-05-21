# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.interfaces.single.RegistrationController.register`
- Node count: 9
- Edge count: 10
- Resolution count: 1
- Boundary count: 1
- Unresolved count: 0

## Nodes

- `class:com.example.interfaces.single.RegistrationController` CLASS com.example.interfaces.single.RegistrationController
- `method:com.example.interfaces.single.RegistrationController.register` METHOD com.example.interfaces.single.RegistrationController.register
- `interface:com.example.interfaces.single.RegistrationUseCase` INTERFACE com.example.interfaces.single.RegistrationUseCase
- `method:com.example.interfaces.single.RegistrationUseCase.create` METHOD com.example.interfaces.single.RegistrationUseCase.create
- `class:com.example.interfaces.single.RegistrationService` CLASS com.example.interfaces.single.RegistrationService
- `method:com.example.interfaces.single.RegistrationService.create` METHOD com.example.interfaces.single.RegistrationService.create
- `method:com.example.interfaces.single.RegistrationService.validate` METHOD com.example.interfaces.single.RegistrationService.validate
- `interface:com.example.interfaces.single.UserRepository` INTERFACE com.example.interfaces.single.UserRepository
- `boundary:com.example.interfaces.single.UserRepository.save` BOUNDARY com.example.interfaces.single.UserRepository.save

## Edges

- `declares:class:com.example.interfaces.single.RegistrationController->method:com.example.interfaces.single.RegistrationController.register` class:com.example.interfaces.single.RegistrationController -> method:com.example.interfaces.single.RegistrationController.register (DECLARES)
- `declares:interface:com.example.interfaces.single.RegistrationUseCase->method:com.example.interfaces.single.RegistrationUseCase.create` interface:com.example.interfaces.single.RegistrationUseCase -> method:com.example.interfaces.single.RegistrationUseCase.create (DECLARES)
- `calls:method:com.example.interfaces.single.RegistrationController.register->method:com.example.interfaces.single.RegistrationUseCase.create:11:1` method:com.example.interfaces.single.RegistrationController.register -> method:com.example.interfaces.single.RegistrationUseCase.create (CALLS)
- `implements:class:com.example.interfaces.single.RegistrationService->interface:com.example.interfaces.single.RegistrationUseCase` class:com.example.interfaces.single.RegistrationService -> interface:com.example.interfaces.single.RegistrationUseCase (IMPLEMENTS)
- `declares:class:com.example.interfaces.single.RegistrationService->method:com.example.interfaces.single.RegistrationService.create` class:com.example.interfaces.single.RegistrationService -> method:com.example.interfaces.single.RegistrationService.create (DECLARES)
- `resolves:method:com.example.interfaces.single.RegistrationUseCase.create->method:com.example.interfaces.single.RegistrationService.create` method:com.example.interfaces.single.RegistrationUseCase.create -> method:com.example.interfaces.single.RegistrationService.create (RESOLVES_TO)
- `declares:class:com.example.interfaces.single.RegistrationService->method:com.example.interfaces.single.RegistrationService.validate` class:com.example.interfaces.single.RegistrationService -> method:com.example.interfaces.single.RegistrationService.validate (DECLARES)
- `calls:method:com.example.interfaces.single.RegistrationService.create->method:com.example.interfaces.single.RegistrationService.validate:15:1` method:com.example.interfaces.single.RegistrationService.create -> method:com.example.interfaces.single.RegistrationService.validate (CALLS)
- `declares:interface:com.example.interfaces.single.UserRepository->boundary:com.example.interfaces.single.UserRepository.save` interface:com.example.interfaces.single.UserRepository -> boundary:com.example.interfaces.single.UserRepository.save (DECLARES)
- `calls:method:com.example.interfaces.single.RegistrationService.create->boundary:com.example.interfaces.single.UserRepository.save:16:2` method:com.example.interfaces.single.RegistrationService.create -> boundary:com.example.interfaces.single.UserRepository.save (CALLS)

## Resolutions

- `INTERFACE_SINGLE_IMPLEMENTATION` `method:com.example.interfaces.single.RegistrationUseCase.create` -> `method:com.example.interfaces.single.RegistrationService.create` evidence=`INFERRED` confidence=`HIGH`

## Boundaries

- `com.example.interfaces.single.UserRepository.save` kind=`REPOSITORY` from=`method:com.example.interfaces.single.RegistrationService.create` reason=`REPOSITORY_BOUNDARY` confidence=`HIGH`

## Unresolved

No unresolved symbols.

## AI Interpretations

None. This artifact contains deterministic facts only.
