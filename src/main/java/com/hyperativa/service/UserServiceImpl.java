package com.hyperativa.service;

import com.hyperativa.dto.AuthRequest;
import com.hyperativa.dto.AuthResponse;
import com.hyperativa.exception.UserAlreadyExistsException;
import com.hyperativa.exception.UserNotFoundException;
import com.hyperativa.model.User;
import com.hyperativa.repository.UserRepository;
import com.hyperativa.service.contract.JwtService;
import com.hyperativa.service.contract.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Attempting to authenticate user: {}", authRequest.getUsername());

        User user = userRepository.findByUsername(authRequest.getUsername())
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", authRequest.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user, user.getId());
        log.info("User authenticated successfully: {}", authRequest.getUsername());

        return new AuthResponse(token, user.getUsername(), user.getId());
    }

    public void registerUser(String username, String password) {
        log.info("Registering new user: {}", username);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("User with username '" + username + "' already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        userRepository.save(user);
        log.info("User registered successfully: {}", username);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}
