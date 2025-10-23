package com.sambrana.oauth2login.controller;

import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ✅ Get logged-in user info
    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(
            Authentication authentication,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String email = principal.getAttribute("email");
        if (email == null) {
            return ResponseEntity.ok(Map.of("authenticated", false, "error", "Missing email from OAuth2 provider"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> userDetails = Map.of(
                    "authenticated", true,
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "bio", user.getBio() == null ? "" : user.getBio(),
                    "avatarUrl", user.getAvatarUrl()
            );
            return ResponseEntity.ok(userDetails);
        } else {
            // First-time login (user record not yet in DB)
            String providerId = "";
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                providerId = oauthToken.getAuthorizedClientRegistrationId();
            }

            String avatarUrl = providerId.equalsIgnoreCase("github")
                    ? principal.getAttribute("avatar_url")
                    : principal.getAttribute("picture");

            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "email", email,
                    "displayName", principal.getAttribute("name"),
                    "avatarUrl", avatarUrl,
                    "bio", ""
            ));
        }
    }

    // ✅ Update profile (requires CSRF + authentication)
    @PostMapping("/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "error", "Not authenticated"));
        }

        String email = principal.getAttribute("email");
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "error", "User not found"));
        }

        User user = userOpt.get();

        user.setDisplayName(request.getDisplayName());
        user.setBio(request.getBio());

        userRepository.save(user);
        logger.info("✅ Profile updated successfully for {}", email);

        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
    }
}
