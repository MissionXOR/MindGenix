package com.stress.config;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "Invalid email or password. Please try again.";

        if (exception.getMessage().equals("Please verify your email first.")) {
            errorMessage = "Please verify your email first.";
        }

        // Set the exception message in the session
        request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", exception);
        setDefaultFailureUrl("/auth/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}