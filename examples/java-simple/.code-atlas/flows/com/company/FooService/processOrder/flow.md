# Java Flow Graph

Schema version: `1.0`

Entrypoint: `com.company.FooService.processOrder`

Generated at: `1970-01-01T00:00:00Z`

## Nodes

| ID | Kind | Qualified name | Display name |
| --- | --- | --- | --- |
| class:com.company.FooService | CLASS | com.company.FooService | FooService |
| method:com.company.FooService.processOrder | METHOD | com.company.FooService.processOrder | processOrder |
| method:com.company.FooService.validate | METHOD | com.company.FooService.validate | validate |
| method:repository.save | METHOD | repository.save | save |
| method:paymentClient.charge | METHOD | paymentClient.charge | charge |
| method:com.company.FooService.mapper | METHOD | com.company.FooService.mapper | mapper |

## Edges

| ID | Kind | Source | Target |
| --- | --- | --- | --- |
| declares:class:com.company.FooService->method:com.company.FooService.processOrder | DECLARES | class:com.company.FooService | method:com.company.FooService.processOrder |
| calls:method:com.company.FooService.processOrder->method:com.company.FooService.validate:1 | CALLS | method:com.company.FooService.processOrder | method:com.company.FooService.validate |
| calls:method:com.company.FooService.processOrder->method:repository.save:2 | CALLS | method:com.company.FooService.processOrder | method:repository.save |
| calls:method:com.company.FooService.processOrder->method:paymentClient.charge:3 | CALLS | method:com.company.FooService.processOrder | method:paymentClient.charge |
| calls:method:com.company.FooService.processOrder->method:com.company.FooService.mapper:4 | CALLS | method:com.company.FooService.processOrder | method:com.company.FooService.mapper |
