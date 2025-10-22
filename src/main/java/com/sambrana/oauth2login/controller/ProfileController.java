package com.sambrana.oauth2login.controller;

import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/"; // not logged in
        }

        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setDisplayName(principal.getAttribute("name"));
        }

        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User userForm, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/";
        }

        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email);

        if (user != null) {
            user.setDisplayName(userForm.getDisplayName());
            user.setBio(userForm.getBio());
            userRepository.save(user);
        }

        return "redirect:/profile";
    }
}
