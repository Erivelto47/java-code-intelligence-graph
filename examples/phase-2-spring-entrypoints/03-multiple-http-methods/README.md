# 03 Multiple HTTP Methods

Validates multiple shortcut mappings in the same controller:

- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`

Expected endpoints:

```text
GET /users -> com.example.spring.multiple.UserController.list
POST /users -> com.example.spring.multiple.UserController.create
PUT /users/{id} -> com.example.spring.multiple.UserController.update
DELETE /users/{id} -> com.example.spring.multiple.UserController.delete
```
