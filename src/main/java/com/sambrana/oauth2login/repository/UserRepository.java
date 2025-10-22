package com.sambrana.oauth2login.repository;

import com.sambrana.oauth2login.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
