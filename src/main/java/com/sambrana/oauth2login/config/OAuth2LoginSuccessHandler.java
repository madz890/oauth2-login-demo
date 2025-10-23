package com.sambrana.oauth2login.config;

import com.sambrana.oauth2login.model.AuthProvider;
import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.repository.AuthProviderRepository;
import com.sambrana.oauth2login.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser; // Use OidcUser for Google
import org.springframework.security.oauth2.core.user.OAuth2User; // Keep for potential non-OIDC providers
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component // Make it a Spring bean
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthProviderRepository authProviderRepository;

    public OAuth2LoginSuccessHandler() {
        // Set the default success URL (redirect target)
        this.setDefaultTargetUrl("http://localhost:3000/profile");
        // Ensure we always redirect, even if there was no saved request
        this.setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    @Transactional // Make this method transactional to ensure saves happen
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        logger.info(">>> OAuth2LoginSuccessHandler: Authentication successful. Processing user...");
        String providerId = ""; // e.g., "google", "github"
        String providerUserId = ""; // The unique ID from the provider
        String email = null;
        String name = null;
        String avatarUrl = null;

        Object principal = authentication.getPrincipal();

        if (authentication instanceof OAuth2AuthenticationToken) {
            providerId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId().toUpperCase();
        }

        // --- Extract user info based on principal type ---
        if (principal instanceof OidcUser) { // Primarily for Google
            OidcUser oidcUser = (OidcUser) principal;
            providerUserId = oidcUser.getSubject();
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
            avatarUrl = oidcUser.getPicture();
            logger.info(">>> OIDC User detected. Provider: {}, Email: {}", providerId, email);
        } else if (principal instanceof OAuth2User) { // Fallback (potentially for GitHub if not using OIDC flow)
            OAuth2User oauth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oauth2User.getAttributes();
            // Adjust attribute names based on provider if necessary
            if ("GITHUB".equals(providerId)) {
                providerUserId = attributes.get("id") != null ? attributes.get("id").toString() : null;
                email = (String) attributes.get("email"); // May still be null
                name = (String) attributes.get("name");
                avatarUrl = (String) attributes.get("avatar_url");
                // Potentially add GitHub email fetch logic here if needed and not done elsewhere
            } else {
                 // Generic fallback - might not work reliably for all providers
                 providerUserId = oauth2User.getName(); // Or another unique attribute
                 email = (String) attributes.get("email");
                 name = (String) attributes.get("name");
                 avatarUrl = (String) attributes.get("picture"); // Common attribute name
            }
             logger.info(">>> OAuth2 User detected. Provider: {}, Email: {}", providerId, email);
        } else {
             logger.error("!!! Unknown principal type: {}", principal.getClass().getName());
             super.onAuthenticationSuccess(request, response, authentication); // Default redirect
             return;
        }


        if (email == null || providerUserId == null) {
            logger.error("!!! Could not extract email ({}) or providerUserId ({}) from principal.", email, providerUserId);
            // Decide how to handle this - maybe redirect to an error page?
            // For now, let's proceed to default redirect, but log the error.
             super.onAuthenticationSuccess(request, response, authentication); // Default redirect
             return;
        }

        // --- Database Logic (similar to CustomOAuth2UserService) ---
        try {
            Optional<AuthProvider> authProviderOpt = authProviderRepository.findByProviderAndProviderUserId(providerId, providerUserId);
            User user; // Declare user here

            if (authProviderOpt.isEmpty()) {
                logger.info("AuthProvider not found for {}. Checking for existing user by email: {}", providerId, email);
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isEmpty()) {
                    logger.info("Creating new user for email: {}", email);
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setDisplayName(name != null ? name : "Unknown");
                    newUser.setAvatarUrl(avatarUrl);
                    // @PrePersist handles timestamps
                    user = userRepository.saveAndFlush(newUser);
                    logger.info("Saved new user with ID: {}", user.getId());
                } else {
                    logger.info("User found by email (ID: {}). Linking provider {}", userOpt.get().getId(), providerId);
                    user = userOpt.get();
                     // Optional: Update details if different
                     user.setDisplayName(name != null ? name : user.getDisplayName());
                     user.setAvatarUrl(avatarUrl != null ? avatarUrl : user.getAvatarUrl());
                     userRepository.save(user); // Save potential updates
                }

                logger.info("Creating new AuthProvider link for user ID: {}", user.getId());
                AuthProvider newAuthProvider = new AuthProvider();
                newAuthProvider.setUser(user);
                newAuthProvider.setProvider(providerId);
                newAuthProvider.setProviderUserId(providerUserId);
                newAuthProvider.setProviderEmail(email); // Save the email used for linking
                authProviderRepository.saveAndFlush(newAuthProvider);
                logger.info("Saved new AuthProvider.");

            } else {
                logger.info("AuthProvider found for {}. Updating existing user...", providerId);
                user = authProviderOpt.get().getUser();
                user.setDisplayName(name != null ? name : user.getDisplayName());
                user.setAvatarUrl(avatarUrl != null ? avatarUrl : user.getAvatarUrl());
                // @PreUpdate handles timestamp
                userRepository.save(user);
                logger.info("Updated existing user ID: {}", user.getId());
            }
             logger.info(">>> User processing complete for email: {}", email);

        } catch (Exception e) {
            logger.error("!!! DATABASE ERROR during onAuthenticationSuccess for email: {}", email, e);
            // Handle error - maybe set an error attribute and redirect differently?
            // For now, just log it. The transaction should roll back.
        }

        // --- Proceed with the redirect ---
        // The SavedRequestAwareAuthenticationSuccessHandler handles the redirect logic
        super.onAuthenticationSuccess(request, response, authentication);
    }
}