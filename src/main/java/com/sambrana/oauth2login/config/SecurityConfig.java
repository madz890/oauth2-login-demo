package com.sambrana.oauth2login.config;

import com.sambrana.oauth2login.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomOAuth2UserService oAuth2UserService;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, CustomOAuth2UserService oAuth2UserService) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/")
                .successHandler(oAuth2LoginSuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
            )
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            // allow frames for H2 console
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // disable CSRF for H2 console
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));

        return http.build();
    }
}
