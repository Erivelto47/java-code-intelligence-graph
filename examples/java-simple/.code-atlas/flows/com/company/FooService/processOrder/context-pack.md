# Context Pack

## Deterministic Facts

- Schema version: `1.0`
- Entrypoint: `com.company.FooService.processOrder`
- Node count: 6
- Edge count: 5

## Nodes

- `class:com.company.FooService` CLASS com.company.FooService
- `method:com.company.FooService.processOrder` METHOD com.company.FooService.processOrder
- `method:com.company.FooService.validate` METHOD com.company.FooService.validate
- `method:repository.save` METHOD repository.save
- `method:paymentClient.charge` METHOD paymentClient.charge
- `method:com.company.FooService.mapper` METHOD com.company.FooService.mapper

## Edges

- `declares:class:com.company.FooService->method:com.company.FooService.processOrder` class:com.company.FooService -> method:com.company.FooService.processOrder (DECLARES)
- `calls:method:com.company.FooService.processOrder->method:com.company.FooService.validate:1` method:com.company.FooService.processOrder -> method:com.company.FooService.validate (CALLS)
- `calls:method:com.company.FooService.processOrder->method:repository.save:2` method:com.company.FooService.processOrder -> method:repository.save (CALLS)
- `calls:method:com.company.FooService.processOrder->method:paymentClient.charge:3` method:com.company.FooService.processOrder -> method:paymentClient.charge (CALLS)
- `calls:method:com.company.FooService.processOrder->method:com.company.FooService.mapper:4` method:com.company.FooService.processOrder -> method:com.company.FooService.mapper (CALLS)

## AI Interpretations

None. This artifact contains deterministic facts only.
