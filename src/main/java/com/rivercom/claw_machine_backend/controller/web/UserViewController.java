package com.rivercom.claw_machine_backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserViewController {

    @GetMapping
    public String usersPage() {
        return "users";
    }
}