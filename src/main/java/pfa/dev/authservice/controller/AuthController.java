package pfa.dev.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pfa.dev.authservice.dto.CurrentUserResponse;
import pfa.dev.authservice.dto.CreateUserRequest;
import pfa.dev.authservice.dto.CreateUserResponse;
import pfa.dev.authservice.dto.EmailRequest;
import pfa.dev.authservice.dto.LoginRequest;
import pfa.dev.authservice.dto.LoginResponse;
import pfa.dev.authservice.dto.LogoutRequest;
import pfa.dev.authservice.dto.RefreshTokenRequest;
import pfa.dev.authservice.dto.ResetPasswordRequest;
import pfa.dev.authservice.dto.SignupRequest;
import pfa.dev.authservice.sevice.KeycloakAdminService;
import pfa.dev.authservice.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAdminService keycloakAdminService;
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        if (keycloakAdminService.userExists(request.getEmail())) {
            return ResponseEntity.badRequest().body("Utilisateur existe deja !");
        }
        return ResponseEntity.ok(keycloakAdminService.createUser(request));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(keycloakAdminService.createUserAccount(request));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        keycloakAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody EmailRequest request) {
        keycloakAdminService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok("Reset password email sent to: " + request.getEmail());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exist")
    public boolean userExists(@RequestParam String email) {
        return keycloakAdminService.userExists(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        keycloakAdminService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully for: " + request.getEmail());
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
