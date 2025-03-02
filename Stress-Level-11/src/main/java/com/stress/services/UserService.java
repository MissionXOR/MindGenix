package com.stress.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stress.entity.User;
import com.stress.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender mailSender; // Inject JavaMailSender

    
    private final String UPLOAD_DIR = "E:/pimage/";


    public User registerUser(MultipartFile file, User user) throws IOException {
        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already exists. Please use a different email.");
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER"); // Assign default role if not provided
        }

        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Handle profile picture upload
        if (!file.isEmpty()) {
            // Create the upload directory if it doesn't exist
            File uploadPath = new File(UPLOAD_DIR);
            if (!uploadPath.exists()) {
                uploadPath.mkdirs(); // ফোল্ডার তৈরি করুন যদি না থাকে
            }

            // Save the file
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, file.getBytes());
            user.setProfilePicture(fileName);
        } else {
            user.setProfilePicture("default.png"); // Default profile picture
        }
     // Generate verification token
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setEnabled(false); // User is not enabled until email is verified

        // Save the user
        User savedUser = userRepository.save(user);

        // Send verification email
        sendVerificationEmail(savedUser);

        return savedUser;
    }
    private void sendVerificationEmail(User user) {
        String verificationLink = "http://localhost:8080/auth/verify?token=" + user.getVerificationToken();
        String subject = "Email Verification - MindGenix";
        String message = "Dear " + user.getUsername() + ",\n\n"
                + "Thank you for registering with MindGenix. Please click the link below to verify your email address:\n\n"
                + verificationLink + "\n\n"
                + "If you did not register with us, please ignore this email.\n\n"
                + "Best regards,\n"
                + "MindGenix Team";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage); // Send the email
    }

    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token);
        if (user != null) {
            user.setEnabled(true);
            user.setVerificationToken(null); // Clear the token after verification
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid verification token.");
        }
    }
    
    public User findByResetToken(String resetToken) {
        return userRepository.findByResetToken(resetToken);
    }

    public void resetPassword(String resetToken, String password) {
        User user = userRepository.findByResetToken(resetToken);
        if (user == null) {
            throw new RuntimeException("Invalid or expired reset token.");
        }

        // Encrypt the new password
        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null); // Clear the reset token
        userRepository.save(user);
    }
   
    public void generateResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found with email: " + email);
        }

        // Generate a reset token
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        userRepository.save(user);

        // Send reset email
        sendResetEmail(user.getEmail(), resetToken);
    }

    private void sendResetEmail(String email, String resetToken) {
        String resetLink = "http://localhost:8080/auth/reset-password?token=" + resetToken;
        String subject = "Password Reset - MindGenix";
        String message = "Dear User,\n\n"
                + "Please click the link below to reset your password:\n\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Best regards,\n"
                + "MindGenix Team";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
    
    

    public User updateUserProfile(User user, MultipartFile file) throws IOException {
        System.out.println("Updating user: " + user.getId()); // Debugging

        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update username
        existingUser.setUsername(user.getUsername());

        // Update password if provided
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Handle new profile picture
        if (file != null && !file.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, file.getBytes());
            existingUser.setProfilePicture(fileName);
        }

        return userRepository.save(existingUser);
    }



    // Find user by email
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Fetch all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Find user by ID
    public User findUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    // Update user information
    public User updateUser(Long id, User updatedUser) {
        User existingUser = findUserById(id); // Use the findUserById method
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(existingUser);
    }

    // Delete a user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}