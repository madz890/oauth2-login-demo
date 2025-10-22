package com.sambrana.oauth2login.repository;

import com.sambrana.oauth2login.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
}
