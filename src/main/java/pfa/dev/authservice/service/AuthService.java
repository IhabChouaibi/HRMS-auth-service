package pfa.dev.authservice.service;

import pfa.dev.authservice.dto.CurrentUserResponse;
import pfa.dev.authservice.dto.LoginRequest;
import pfa.dev.authservice.dto.LoginResponse;
import pfa.dev.authservice.dto.LogoutRequest;
import pfa.dev.authservice.dto.RefreshTokenRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    CurrentUserResponse getCurrentUser();
}
