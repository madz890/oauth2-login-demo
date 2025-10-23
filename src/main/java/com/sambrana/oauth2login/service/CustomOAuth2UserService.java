package com.sambrana.oauth2login.service;

import com.sambrana.oauth2login.model.User;
import com.sambrana.oauth2login.model.AuthProvider;
import com.sambrana.oauth2login.repository.UserRepository;
import com.sambrana.oauth2login.repository.AuthProviderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary; // <-- IMPORT @Primary
import org.springframework.core.ParameterizedTypeReference;
// --- Use the GENERIC imports again ---
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
// --- END GENERIC imports ---
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Primary // <-- ADD @Primary annotation to prioritize this bean
// --- Implement the GENERIC interface again ---
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthProviderRepository authProviderRepository;

    @PostConstruct
    public void init() {
        System.out.println("<<<<<<<< CustomOAuth2UserService BEAN CREATED SUCCESSFULLY >>>>>>>>");
        logger.info("<<<<<<<< CustomOAuth2UserService BEAN CREATED SUCCESSFULLY (via logger) >>>>>>>>");
    }

    // --- Method signature uses GENERIC types again ---
    // --- (Your imports remain the same) ---

// ...

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Use the simple System.out.println for initial check
        System.out.println("***** EXECUTING CustomOAuth2UserService.loadUser (GENERIC VERSION) *****");
        logger.info(">>> Entered loadUser method (GENERIC). Attempting to load default user...");

        // Use the DefaultOAuth2UserService
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        logger.info(">>> Default user loaded successfully. Starting transaction block. Is transaction active? {}", TransactionSynchronizationManager.isActualTransactionActive());

        // --- CHANGE 1: Make a MUTABLE copy of the attributes ---
        Map<String, Object> attributes = new java.util.HashMap<>(oAuth2User.getAttributes());

        String finalEmail = null;

        try {
            String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
            
            // We no longer get attributes here, we use the mutable map from above
            logger.info(">>> Inside try block. Provider: {}. Attributes obtained.", provider);

            String providerUserId;
            String email = (String) attributes.get("email"); // Read from our mutable map
            String name;
            String avatarUrl;

            // Provider specific attribute fetching logic
            if ("GITHUB".equals(provider)) {
                providerUserId = attributes.get("id").toString();
                name = (String) attributes.get("name");
                avatarUrl = (String) attributes.get("avatar_url");
                
                // --- CHANGE 2: Update the attributes map if we fetch the email ---
                if (email == null) {
                    logger.info(">>> GitHub email is null, attempting to fetch from API...");
                    String fetchedEmail = getEmailFromGitHub(userRequest.getAccessToken().getTokenValue());
                    if (fetchedEmail != null) {
                        email = fetchedEmail;
                        attributes.put("email", email); // <-- This is the critical fix
                        logger.info(">>> Successfully fetched and ADDED email to attributes: {}", email);
                    }
                }
            } else if ("GOOGLE".equals(provider)) {
                 // For Google with generic service, attributes contain standard claims
                 providerUserId = (String) attributes.get("sub"); // Use 'sub' claim
                 name = (String) attributes.get("name");
                 avatarUrl = (String) attributes.get("picture");
                 // email should already be populated
            } else {
                 logger.error("Unsupported provider detected: {}", provider);
                 throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
            }


            if (email == null) {
                 logger.error("Email is STILL null after fetching. ProviderUserId: {}", providerUserId);
                 throw new OAuth2AuthenticationException("Email not found from provider");
            }

            finalEmail = email;

            // ... (All your logic for saving the User and AuthProvider is PERFECT, no changes needed) ...
            logger.info(">>> Processing user. Email: {}, Provider: {}, ProviderUserId: {}", finalEmail, provider, providerUserId);
 
            Optional<AuthProvider> authProviderOpt = authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);
            logger.info(">>> Checked for existing AuthProvider. Found: {}", authProviderOpt.isPresent());
 
            User user; // Declare user variable here
 
            if (authProviderOpt.isEmpty()) {
 
                logger.info("AuthProvider not found. Checking for existing user by email: {}", finalEmail);
                Optional<User> userOpt = userRepository.findByEmail(finalEmail);
                logger.info(">>> Checked for existing User. Found: {}", userOpt.isPresent());
 
                if (userOpt.isEmpty()) {
                    logger.info("User not found by email. Creating new user for: {}", finalEmail);
                    User newUser = new User();
                    newUser.setEmail(finalEmail);
                    newUser.setDisplayName(name != null ? name : "Unknown");
                    newUser.setAvatarUrl(avatarUrl);
 
                    logger.info("Attempting saveAndFlush for new user...");
                    user = userRepository.saveAndFlush(newUser);
                    logger.info("Successfully SAVED new user with ID: {}", user.getId());
                } else {
                    logger.info("User already exists with ID: {}, linking provider.", userOpt.get().getId());
                    user = userOpt.get();
                    user.setDisplayName(name != null ? name : user.getDisplayName());
                    user.setAvatarUrl(avatarUrl != null ? avatarUrl : user.getAvatarUrl());
                    userRepository.save(user); // Save potential updates
                }
 
                logger.info("Attempting saveAndFlush for new AuthProvider for user ID: {}", user.getId());
                AuthProvider newAuthProvider = new AuthProvider();
                newAuthProvider.setUser(user);
                newAuthProvider.setProvider(provider);
                newAuthProvider.setProviderUserId(providerUserId);
                newAuthProvider.setProviderEmail(finalEmail);
 
                authProviderRepository.saveAndFlush(newAuthProvider);
                logger.info("Successfully SAVED auth provider for user ID: {}", user.getId());
 
            } else {
                logger.info("AuthProvider found. Updating existing user ID: {}", authProviderOpt.get().getUser().getId());
                user = authProviderOpt.get().getUser();
                user.setDisplayName(name != null ? name : user.getDisplayName());
                user.setAvatarUrl(avatarUrl != null ? avatarUrl : user.getAvatarUrl());
                userRepository.save(user);
                logger.info("Successfully updated existing user ID: {}", user.getId());
            }

            logger.info(">>> Reached end of try block. Registering after-commit action.");

            final String emailForLogging = finalEmail;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info(">>> TRANSACTION COMMITTED SUCCESSFULLY for email: {}", emailForLogging);
                }
                @Override
                public void afterCompletion(int status) {
                     if (status != STATUS_COMMITTED) {
                         logger.error("XXX TRANSACTION FAILED OR ROLLED BACK for email: {}. Status: {}", emailForLogging, status == STATUS_ROLLED_BACK ? "ROLLED_BACK" : "UNKNOWN");
                     }
                }
            });

        } catch (Exception ex) {
            logger.error("!!! FAILED TO PROCESS USER LOGIN INSIDE CATCH BLOCK !!!", ex);
            OAuth2Error error = new OAuth2Error("DATABASE_SAVE_ERROR", "Failed to save user: " + ex.getMessage(), null);
            throw new OAuth2AuthenticationException(error, ex);
        }
         logger.info(">>> Exiting loadUser method successfully (before commit). Is transaction active? {}", TransactionSynchronizationManager.isActualTransactionActive());

        // --- CHANGE 3: Return a NEW user object with the MODIFIED attributes ---
        
        // Get the "name" attribute key (e.g., "sub" for Google, "id" for GitHub)
        String nameAttributeKey = userRequest.getClientRegistration()
                                            .getProviderDetails()
                                            .getUserInfoEndpoint()
                                            .getUserNameAttributeName();

        // Return a new DefaultOAuth2User instance
        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
            oAuth2User.getAuthorities(),
            attributes, // Use our (potentially modified) attributes map
            nameAttributeKey
        );
    }

// ... (Your getEmailFromGitHub method stays the same) ...

    // getEmailFromGitHub helper function remains the same
    private String getEmailFromGitHub(String token) {
        String email = null;
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ParameterizedTypeReference<List<Map<String, Object>>> responseType =
                new ParameterizedTypeReference<List<Map<String, Object>>>() {};

            ResponseEntity<List<Map<String, Object>>> resp = rt.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            List<Map<String, Object>> emails = resp.getBody();
            if (emails != null) {
                Optional<Map<String, Object>> primary = emails.stream().filter(m ->
                    Boolean.TRUE.equals(m.get("primary")) && Boolean.TRUE.equals(m.get("verified"))
                ).findFirst();

                if (!primary.isPresent()) {
                    primary = emails.stream().filter(m ->
                        Boolean.TRUE.equals(m.get("verified"))
                    ).findFirst();
                }

                if (primary.isPresent()) {
                    email = primary.get().get("email").toString();
                     logger.info(">>> Successfully fetched primary/verified email from GitHub: {}", email);
                } else {
                     logger.warn(">>> Could not find primary/verified email from GitHub response.");
                }
            }
        } catch (Exception ex) {
           logger.error(">>> Failed to fetch email from GitHub API", ex);
        }
        return email;
    }
}