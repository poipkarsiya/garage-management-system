
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.User;
import com.garage.garagemanagementsystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user != null
                && user.getPassword().equals(password)
                && Boolean.TRUE.equals(user.getEnabled())) {

            session.setAttribute("loggedInUser", user);
            session.setAttribute("userRole",
                    user.getRole().toUpperCase());

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/dashboard";
            } else if ("USER".equalsIgnoreCase(user.getRole())) {
                return "redirect:/user-home";
            }
        }

        model.addAttribute("error",
                "Invalid username, password, or account role.");

        return "login";
    }

    @GetMapping("/user-home")
    public String userHome(HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"USER".equalsIgnoreCase(user.getRole())) {
            return "redirect:/dashboard";
        }

        return "user-home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
