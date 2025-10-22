package com.sambrana.oauth2login.model;

import jakarta.persistence.*;

@Entity
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id") // ✅ still fine
    private User user;

    private String provider;
    private String providerUserId;
    private String providerEmail;

    // Getters and Setters
    public Long getId() { return id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
