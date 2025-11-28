package com.hyperativa.service.contract;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateToken(UserDetails userDetails, Long userId);

    String extractUsername(String token);

    Long extractUserId(String token);

    boolean isTokenValid(String token);
}
