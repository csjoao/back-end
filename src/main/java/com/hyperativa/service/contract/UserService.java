package com.hyperativa.service.contract;

import com.hyperativa.dto.AuthRequest;
import com.hyperativa.dto.AuthResponse;
import com.hyperativa.model.User;

import java.util.Optional;

public interface UserService {

    AuthResponse authenticate(AuthRequest authRequest);

    void registerUser(String username, String password);

    Optional<User> findById(Long userId);
}
