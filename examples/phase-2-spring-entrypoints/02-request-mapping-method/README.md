# 02 Request Mapping Method

Validates method-level `@RequestMapping` with an explicit
`RequestMethod.GET`.

Expected endpoint:

```text
GET /users/{id} -> com.example.spring.requestmapping.UserController.getById
```
