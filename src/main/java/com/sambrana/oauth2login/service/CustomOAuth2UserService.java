package com.sambrana.oauth2login.service;

import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId().toUpperCase();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        // If provider is GitHub and email is missing, fetch emails endpoint using the access token
        if (email == null && "GITHUB".equals(provider)) {
            String token = request.getAccessToken().getTokenValue();
            try {
                RestTemplate rt = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<List> resp = rt.exchange(
                        "https://api.github.com/user/emails",
                        HttpMethod.GET,
                        entity,
                        List.class
                );

                List<?> emails = resp.getBody();
                if (emails != null) {
                    Optional<?> primary = emails.stream().filter(obj -> {
                        if (!(obj instanceof Map)) return false;
                        Map<?, ?> m = (Map<?, ?>) obj;
                        Object primaryFlag = m.get("primary");
                        Object verifiedFlag = m.get("verified");
                        return Boolean.TRUE.equals(primaryFlag) && Boolean.TRUE.equals(verifiedFlag);
                    }).findFirst();

                    if (!primary.isPresent()) {
                        primary = emails.stream().filter(obj -> {
                            if (!(obj instanceof Map)) return false;
                            Map<?, ?> m = (Map<?, ?>) obj;
                            return Boolean.TRUE.equals(m.get("verified"));
                        }).findFirst();
                    }

                    if (primary.isPresent()) {
                        Map<?, ?> m = (Map<?, ?>) primary.get();
                        Object e = m.get("email");
                        if (e != null) {
                            email = e.toString();
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore and fall through to error below
            }
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from provider");
        }

        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setDisplayName((String) attributes.getOrDefault("name", "Unknown"));
            newUser.setAvatarUrl((String) attributes.getOrDefault("picture", ""));
            userRepository.save(newUser);
        }

        return oAuth2User;
    }
}
