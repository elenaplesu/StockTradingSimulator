package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthenticationManager {

    private final UserService userService;

    public AuthenticationManager(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(name="registered", required=false) String registered) {

        if (registered != null) {
            model.addAttribute("message", "Registration successful! Please log in.");
        }

        return "login";
    }
    //  Process the Login Form
    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        User loggedInUser = userService.authenticateUser(username, password);

        if (loggedInUser != null) {
            session.setAttribute("user", loggedInUser);
            return "redirect:/explore";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    // Process Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destroy the session memory
        return "redirect:/"; // Send back to home page
    }

    // Shows the registration HTML page
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               Model model) {
        String result = userService.registerNewUser(username, password);
        if (result.equals("Success")) {
            // If successful, send them to the login page (we will build login next)
            return "redirect:/login?registered=true";
        } else {
            // If error (like username taken), reload page and show error message
            model.addAttribute("error", result);
            return "register";
        }
    }
}
