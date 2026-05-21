# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.example.repository.AccountService.openAccount`
- Node count: 5
- Edge count: 5
- Resolution count: 0
- Boundary count: 2
- Unresolved count: 0

## Nodes

- `class:com.example.repository.AccountService` CLASS com.example.repository.AccountService
- `method:com.example.repository.AccountService.openAccount` METHOD com.example.repository.AccountService.openAccount
- `interface:com.example.repository.AccountRepository` INTERFACE com.example.repository.AccountRepository
- `boundary:com.example.repository.AccountRepository.findByDocument` BOUNDARY com.example.repository.AccountRepository.findByDocument
- `boundary:com.example.repository.AccountRepository.save` BOUNDARY com.example.repository.AccountRepository.save

## Edges

- `declares:class:com.example.repository.AccountService->method:com.example.repository.AccountService.openAccount` class:com.example.repository.AccountService -> method:com.example.repository.AccountService.openAccount (DECLARES)
- `declares:interface:com.example.repository.AccountRepository->boundary:com.example.repository.AccountRepository.findByDocument` interface:com.example.repository.AccountRepository -> boundary:com.example.repository.AccountRepository.findByDocument (DECLARES)
- `calls:method:com.example.repository.AccountService.openAccount->boundary:com.example.repository.AccountRepository.findByDocument:11:1` method:com.example.repository.AccountService.openAccount -> boundary:com.example.repository.AccountRepository.findByDocument (CALLS)
- `declares:interface:com.example.repository.AccountRepository->boundary:com.example.repository.AccountRepository.save` interface:com.example.repository.AccountRepository -> boundary:com.example.repository.AccountRepository.save (DECLARES)
- `calls:method:com.example.repository.AccountService.openAccount->boundary:com.example.repository.AccountRepository.save:12:2` method:com.example.repository.AccountService.openAccount -> boundary:com.example.repository.AccountRepository.save (CALLS)

## Resolutions

No inferred resolutions.

## Boundaries

- `com.example.repository.AccountRepository.findByDocument` kind=`REPOSITORY` from=`method:com.example.repository.AccountService.openAccount` reason=`REPOSITORY_BOUNDARY` confidence=`HIGH`
- `com.example.repository.AccountRepository.save` kind=`REPOSITORY` from=`method:com.example.repository.AccountService.openAccount` reason=`REPOSITORY_BOUNDARY` confidence=`HIGH`

## Unresolved

No unresolved symbols.

## AI Interpretations

None. This artifact contains deterministic facts only.
