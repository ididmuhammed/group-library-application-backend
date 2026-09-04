package com.library.lms.service;

import com.library.lms.dto.request.LoginRequest;
import com.library.lms.dto.request.RefreshTokenRequest;
import com.library.lms.dto.response.AuthResponse;
import com.library.lms.exception.BadRequestException;
import com.library.lms.security.JwtService;
import com.library.lms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid username or password");
        } catch (DisabledException e) {
            throw new BadRequestException("This account has been disabled");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.of(accessToken, refreshToken, principal.getId(),
                principal.getUsername(), principal.getAuthorityNames());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtService.isRefreshToken(token)) {
            throw new BadRequestException("Provided token is not a valid refresh token");
        }

        String username = jwtService.extractUsername(token);
        if (!jwtService.isTokenValid(token, username)) {
            throw new BadRequestException("Refresh token is invalid or expired");
        }

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateAccessToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.of(newAccessToken, newRefreshToken, principal.getId(),
                principal.getUsername(), principal.getAuthorityNames());
    }
}
