package pfa.dev.authservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String username;
    private List<String> roles;

}
