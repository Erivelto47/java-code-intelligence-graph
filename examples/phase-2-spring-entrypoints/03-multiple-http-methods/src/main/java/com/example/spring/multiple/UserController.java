package com.example.spring.multiple;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/users")
public class UserController {
    @GetMapping
    public String list() {
        return "list";
    }

    @PostMapping
    public String create() {
        return "create";
    }

    @PutMapping(value = "/{id}")
    public String update() {
        return "update";
    }

    @DeleteMapping("/{id}")
    public void delete() {
    }
}
