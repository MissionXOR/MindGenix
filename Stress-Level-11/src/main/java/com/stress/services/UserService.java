package com.stress.services;

import org.springframework.beans.factory.annotation.Autowired;
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

        // Save the user to the database
        return userRepository.save(user);
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