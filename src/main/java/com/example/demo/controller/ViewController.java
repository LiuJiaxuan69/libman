package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequestMapping("/view")
public class ViewController {
    @GetMapping("login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("index")
    public String index() {
        return "index";
    }

    @GetMapping("donate")
    public String donate() {
        return "donate";
    }

    @GetMapping("categories")
    public String categories() {
        return "categories";
    }

    @GetMapping("profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("ai")
    public String ai() {
        return "ai";
    }

    @GetMapping("manageBooks")
    public String manageBooks() {
        return "manage_books";
    }
}
