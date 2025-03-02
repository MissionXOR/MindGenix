package com.stress.controller;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;

import com.stress.entity.*;
import com.stress.repository.UserRepository;
import com.stress.services. *;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private StressResultService stressResultService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    // Show Login Page
    @GetMapping("/index")
    public String showPage() {
        return "index";
    }
   
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            HttpSession session, Model model) {

        if (error != null) {
            // Retrieve the error message from the session
            String errorMessage = (String) session.getAttribute("errorMessage");
            if (errorMessage != null) {
                model.addAttribute("errorMessage", errorMessage);
                session.removeAttribute("errorMessage"); // Clear the session attribute
            }
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User()); // Add an empty User object to the model
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, 
                               @RequestParam("file") MultipartFile file, 
                               Model model) {
        try {
            userService.registerUser(file, user); // Save the user with profile picture
            return "redirect:/auth/login"; // Redirect to login page after registration
        } catch (RuntimeException | IOException e) {
            model.addAttribute("errorMessage", e.getMessage()); // Pass error message to the view
            return "register"; // Return to the registration page with error message
        }
    }
    @GetMapping("/verify")
    public String verifyUser(@RequestParam("token") String token, Model model) {
        try {
            userService.verifyUser(token);
            model.addAttribute("message", "Email verified successfully. You can now login.");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "verify"; // Return to the verification result page
    }
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        try {
            userService.generateResetToken(email);
            model.addAttribute("successMessage", "A password reset link has been sent to your email.");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "forgot-password";
    }
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        User user = userService.findByResetToken(token);
        if (user == null) {
            model.addAttribute("errorMessage", "Invalid or expired reset token.");
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "reset-password";
        }

        try {
            userService.resetPassword(token, password);
            model.addAttribute("successMessage", "Your password has been reset successfully.");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "reset-password";
    }
    
    @GetMapping("/profile")
    public String userProfile(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("user", user);
        return "userprofile";
    }

    @PostMapping("/profile/update")
    public String updateUserProfile(@ModelAttribute User user,
                                   @RequestParam("file") MultipartFile file,
                                   Model model) {
        try {
            // Debug: Print the user ID
            System.out.println("User ID from form: " + user.getId());

            if (user.getId() == null) {
                throw new RuntimeException("User ID cannot be null.");
            }

            userService.updateUserProfile(user, file);
            return "redirect:/auth/profile"; // Redirect back to the profile page
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error updating profile.");
            return "userprofile";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "userprofile";
        }
    }



    @GetMapping("/eventCode")
    public String showEventCodePage(Model model, Principal principal) {
        // Fetch the currently logged-in user
        User user = userService.findByEmail(principal.getName());

        // Add the user object to the model
        model.addAttribute("user", user);

        return "eventcode";
    }

    
    @GetMapping("/questions")
    public String showQuestionsPage(Model model) {
        List< Question > questionList = questionService.generateQuestions();
        model.addAttribute("questionList", questionList);
        return "questions";
    }
    @PostMapping("/eventCode")
    public String submitEventCode(@RequestParam String eventCode, Model model) {
        Event event = eventService.findByEventCode(eventCode);
        if (event != null) {
            return "redirect:/auth/questions";
        }
        model.addAttribute("error", "Invalid event code");
        return "eventCode";
    }
    
    
    @PostMapping("/submitAnswers")
    public String submitAnswers(@RequestParam Map<String, String> formData, HttpSession session) {
        // Get user from session
        User user = (User) session.getAttribute("user");

        // If user is not in session, retrieve from Spring Security context
        if (user == null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                user = userService.findByEmail(email);
                session.setAttribute("user", user); // Update session
            } else {
                throw new RuntimeException("User not found in session. Please log in again.");
            }
        }

        // Map answers to scores
        Map<String, Integer> answerScoreMapping = Map.of(
            "Never", 1,
            "Sometimes", 3,
            "Often", 5
        );

        StressResult stressResult = new StressResult();
        Map<String, Integer> responses = new HashMap<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("physical") || key.startsWith("mental") || key.startsWith("job")) {
                Integer score = answerScoreMapping.get(value);
                if (score != null) {
                    responses.put(key, score);
                } else {
                    throw new IllegalArgumentException("Invalid answer: " + value);
                }
            }
        }

        stressResult.setResponses(responses);
        stressResult = stressResultService.calculateStress(stressResult, user);
        session.setAttribute("stressResult", stressResult);

        return "redirect:/auth/result";
    }

    @GetMapping("/result")
    public String getResult(HttpSession session, Model model) {
        StressResult stressResult = (StressResult) session.getAttribute("stressResult");
        if (stressResult == null) {
            return "redirect:/"; // Redirect to home if no data
        }

        model.addAttribute("stressResult", stressResult);
        session.removeAttribute("stressResult");

        return "result";
    }
    @GetMapping("/history")
    public String showUserHistory(@RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            Model model) {
        // Get the logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Fetch the email from the logged-in user

        // Fetch the user object from the database
        User user = userService.findByEmail(email); // Ensure this method exists in your UserService
        if (user == null) {
            throw new RuntimeException("User not found"); // Handle the case where the user is not found
        }

        // Add the user object to the model
        model.addAttribute("user", user);

        // Parse the date range if provided
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDate = LocalDate.parse(startDateStr); // Parse the start date
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDate = LocalDate.parse(endDateStr); // Parse the end date
        }

        List<StressResult> stressHistory;

        if (startDate != null && endDate != null) {
            // Fetch user's stress history by date range
            stressHistory = stressResultService.getUserHistoryByDateRange(email, startDate, endDate);
        } else {
            // Fetch all user's stress history
            stressHistory = stressResultService.getUserHistory(email);
        }

        // Add the stress history to the model
        model.addAttribute("stressHistory", stressHistory);

        return "history"; // Return the history.html template
    }
				    
    /*......................................................................................................*/
    
    @GetMapping("/adminDashboard")
    public String showAdminDashboard(Model model) {
        model.addAttribute("event", new Event());
        return "adminDashboard";
    }
    @PostMapping("/adminDashboard")
    public String createEvent(@ModelAttribute Event event) {
        // Get the currently authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Fetch the email from the logged-in user

        User user = userService.findByEmail(email);

        event.setUser(user);
        eventService.createEvent(event);

        return "redirect:/auth/adminDashboard"; // Redirect back to dashboard after saving
    }
    
    @GetMapping("/admin/users")
    public String manageUsers(Model model) {
    	 List<User> users = userService.getAllUsers();
    	    model.addAttribute("users", users);
    	    return "manage-users"; // Ensure this matches the template name
    }
   

    @GetMapping("/admin/users/edit/{id}")
    public String showUpdateForm(@PathVariable Long id, Model model) {
        User user = userService.findUserById(id);
        model.addAttribute("user", user);
        return "update-user"; // Updated to return "update-user" instead of "admin/update-user"
    }

    // Update user
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User updatedUser) {
        userService.updateUser(id, updatedUser);
        return "redirect:/auth/admin/users"; // Redirect to the manage users page
    }

    // Delete user
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/auth/admin/users"; // Redirect to the manage users page
    }
    
    @GetMapping("/admin/user/result/{userId}")
    public String viewUserResult(@PathVariable Long userId, Model model) {
        List<StressResult> userStressResults = stressResultService.userStressResults(userId);
        model.addAttribute("userStressResults", userStressResults);
        
        // Fetch the user's information (e.g., username) using the userId
        User user = userService.findUserById(userId); // Assuming you have a method to fetch user info
        model.addAttribute("username", user.getUsername()); // Add the username to the model
        
        return "stress-history"; // Thymeleaf template name for the user's stress results page
    }
    
    @GetMapping("/admin/events")
    public String getAllEvents(Model model) {
        // Fetch all events from the service
        List<Event> events = eventService.getAllEvents();
        // Add the list of events to the model
        model.addAttribute("events", events);
        // Return the view name (assuming you are using Thymeleaf)
        return "admin-events";  // This is the path to the Thymeleaf template
    }
    @PostMapping("/admin/events/delete/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return "redirect:/auth/admin/events";
    }
    
    /*............................................................................................*/
    
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // সেশন বন্ধ করা
        return "redirect:/auth/login?logout=true";
    }

    
}