# 01 Simple Rest Controller

Validates discovery of a Spring MVC endpoint declared with:

- `@RestController`
- class-level `@RequestMapping("/auth")`
- method-level `@PostMapping("/register")`

Expected endpoint:

```text
POST /auth/register -> com.example.spring.simple.AuthController.register
```
