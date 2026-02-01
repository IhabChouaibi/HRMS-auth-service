package pfa.dev.authservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pfa.dev.authservice.dto.*;
import pfa.dev.authservice.sevice.KeycloakAdminService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAdminService keycloakAdminService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        if (keycloakAdminService.userExists(request.getEmail())) {
            return ResponseEntity.badRequest().body("Utilisateur existe déjà !");
        }
        return ResponseEntity.ok(keycloakAdminService.createUser(request));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody EmailRequest request) {
        try {
            keycloakAdminService.sendResetPasswordEmail(request.getEmail());
            return ResponseEntity.ok("Reset password email sent to: " + request.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with email: " + request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending reset password email");
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            System.out.println(request.toString());

            LoginResponse response = keycloakAdminService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Nom d'utilisateur ou mot de passe incorrect");
        }
    }
    @GetMapping("/exist")
    public boolean userExists(@RequestParam String email) {
       return  keycloakAdminService.userExists(email);

    }



    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {

        try {
            keycloakAdminService.resetPassword(resetPasswordRequest.getEmail(), resetPasswordRequest.getNewPassword());
            return ResponseEntity.ok("Password reset successfully for: " + resetPasswordRequest.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.status(org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND)
                    .body("User not found with email: " + resetPasswordRequest.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resetting password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody EmailRequest request){
        try{
            keycloakAdminService.logout(request.getEmail());
            return ResponseEntity.ok("Logout successfully for: " + request.getEmail());
        }catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with email: " + request.getEmail());
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error logging out");
        }
    }


}