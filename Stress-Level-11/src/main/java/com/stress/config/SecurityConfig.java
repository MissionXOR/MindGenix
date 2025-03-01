package com.stress.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomSuccessHandler customSuccessHandler;
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/","/auth/login","/auth/index", "/auth/register","/auth/verify", "/login", "/register").permitAll() // Public access
                .requestMatchers("/auth/adminDashboard").hasRole("ADMIN") // Only Admin can access
                .requestMatchers("/auth/eventCode").hasRole("USER") // Only User can access
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() 
                .anyRequest().authenticated() // All other requests require authentication
            )
            .formLogin(form -> form
            	    .loginPage("/auth/login")
            	    .loginProcessingUrl("/auth/userlogin")
            	    .usernameParameter("email") // Accept 'email' instead of 'username'
            	    .passwordParameter("password")
            	    .successHandler(customSuccessHandler)
            	    .failureHandler(customAuthenticationFailureHandler) 
            	    .permitAll()
            	)

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .permitAll()
            );
        http.authenticationProvider(customAuthenticationProvider);
        return http.build();
    }
}