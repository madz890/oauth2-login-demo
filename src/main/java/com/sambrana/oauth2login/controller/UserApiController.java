package com.sambrana.oauth2login.controller;

import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserApiController {

    private final UserRepository userRepository;

    public UserApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> out = new HashMap<>();
        if (principal == null) {
            out.put("authenticated", false);
            return out;
        }

        String email = principal.getAttribute("email");
        out.put("authenticated", true);
        out.put("providerAttributes", principal.getAttributes());

        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email);
        }

        if (user != null) {
            out.put("email", user.getEmail());
            out.put("displayName", user.getDisplayName());
            out.put("avatarUrl", user.getAvatarUrl());
            out.put("bio", user.getBio());
        } else {
            // fall back to attributes
            out.put("email", email);
            out.put("displayName", principal.getAttribute("name"));
            out.put("avatarUrl", principal.getAttribute("picture"));
            out.put("bio", "");
        }

        return out;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        Map<String, String> m = new HashMap<>();
        if (token != null) {
            m.put("headerName", token.getHeaderName());
            m.put("parameterName", token.getParameterName());
            m.put("token", token.getToken());
        }
        return m;
    }
}
