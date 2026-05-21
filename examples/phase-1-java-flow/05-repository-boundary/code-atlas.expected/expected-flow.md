# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.example.repository.AccountService.openAccount`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.example.repository.AccountService | CLASS | com.example.repository.AccountService | AccountService |
| method:com.example.repository.AccountService.openAccount | METHOD | com.example.repository.AccountService.openAccount | openAccount |
| interface:com.example.repository.AccountRepository | INTERFACE | com.example.repository.AccountRepository | AccountRepository |
| boundary:com.example.repository.AccountRepository.findByDocument | BOUNDARY | com.example.repository.AccountRepository.findByDocument | findByDocument |
| boundary:com.example.repository.AccountRepository.save | BOUNDARY | com.example.repository.AccountRepository.save | save |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.example.repository.AccountService->method:com.example.repository.AccountService.openAccount | DECLARES | class:com.example.repository.AccountService | method:com.example.repository.AccountService.openAccount |
| declares:interface:com.example.repository.AccountRepository->boundary:com.example.repository.AccountRepository.findByDocument | DECLARES | interface:com.example.repository.AccountRepository | boundary:com.example.repository.AccountRepository.findByDocument |
| calls:method:com.example.repository.AccountService.openAccount->boundary:com.example.repository.AccountRepository.findByDocument:11:1 | CALLS | method:com.example.repository.AccountService.openAccount | boundary:com.example.repository.AccountRepository.findByDocument |
| declares:interface:com.example.repository.AccountRepository->boundary:com.example.repository.AccountRepository.save | DECLARES | interface:com.example.repository.AccountRepository | boundary:com.example.repository.AccountRepository.save |
| calls:method:com.example.repository.AccountService.openAccount->boundary:com.example.repository.AccountRepository.save:12:2 | CALLS | method:com.example.repository.AccountService.openAccount | boundary:com.example.repository.AccountRepository.save |

## Resolutions

No inferred resolutions.

## Boundaries

| Symbol | Kind | From | Reason | Confidence |
| --- | --- | --- | --- | --- |
| com.example.repository.AccountRepository.findByDocument | REPOSITORY | method:com.example.repository.AccountService.openAccount | REPOSITORY_BOUNDARY | HIGH |
| com.example.repository.AccountRepository.save | REPOSITORY | method:com.example.repository.AccountService.openAccount | REPOSITORY_BOUNDARY | HIGH |

## Unresolved

No unresolved symbols.
