package com.stress.config;

import com.stress.entity.User;
import com.stress.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();

        // ইউজারের ইমেইল পাওয়া
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        // ডাটাবেজ থেকে ইউজার খুঁজে বের করা
        User user = userRepository.findByEmail(email);

        // ইউজার সেশনে সেট করা
        session.setAttribute("user", user);

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/auth/adminDashboard");
        } else if (roles.contains("ROLE_USER")) {
            response.sendRedirect("/auth/eventCode");
        } else {
            response.sendRedirect("/auth/login?error=true");
        }
        
    }
}
