package com.vetautet.infrastructure.config;

import com.vetautet.infrastructure.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/auth/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/google")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/register")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/verify-email")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/resend-verification-otp")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/forgot-password/request")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/forgot-password/reset")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/refresh")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/logout")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/trips/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/stations/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/promotions/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/payments/momo/ipn")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/payments/momo/return")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/payments/vnpay/return")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/payments/vnpay/ipn")).permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(new AntPathRequestMatcher("/upload/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/test-kafka")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/hello")).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://*.ngrok-free.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
