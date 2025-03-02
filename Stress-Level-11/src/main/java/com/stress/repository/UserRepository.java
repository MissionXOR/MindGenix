package com.stress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.stress.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email); // Find user by email
    List<User> findAll(); // Fetch all users
    Optional<User> findById(Long id); // Find user by ID
    User findByVerificationToken(String verificationToken);
    User findByResetToken(String resetToken); // Find user by reset token
}