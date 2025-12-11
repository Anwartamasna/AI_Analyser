package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.UserRepository;
import com.example.resumeanalyzer.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
            PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        System.out.println("Login attempt for user: " + username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            System.out.println("Login success for user: " + username);
            return ResponseEntity.ok(Map.of("token", jwt, "username", username));
        } catch (Exception e) {
            System.err.println("Login failed for user: " + username + " Error: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> signUpRequest) {
        String username = signUpRequest.get("username");
        String password = signUpRequest.get("password");
        System.out.println("Registering user: " + username);

        if (userRepository.existsByUsername(username)) {
            System.out.println("Username already exists: " + username);
            return ResponseEntity.badRequest().body(Map.of("error", "Username is already taken!"));
        }

        User user = new User(username, encoder.encode(password));
        userRepository.save(user);
        System.out.println("User saved to DB: " + username);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }
}
