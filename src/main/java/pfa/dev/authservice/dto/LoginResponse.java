package pfa.dev.authservice.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

}
