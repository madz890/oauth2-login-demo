package com.sambrana.oauth2login.repository;

import com.sambrana.oauth2login.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
    
    Optional<AuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
}