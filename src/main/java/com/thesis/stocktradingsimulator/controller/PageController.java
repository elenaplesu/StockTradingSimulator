package com.thesis.stocktradingsimulator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/")
    public String home(Model model) {
        return "home"; // Loads home.html
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        return "explore"; // Loads explore.html
    }

    @GetMapping("/learn")
    public String learn(Model model) {
        return "learn"; // Loads learn.html
    }

    // Placeholder for Login (we will build this later)
    @GetMapping("/login")
    public String login() {
        return "home"; // Redirect to home for now
    }

    @GetMapping("/register")
    public String register() {
        return "home"; // Redirect to home for now
    }
}