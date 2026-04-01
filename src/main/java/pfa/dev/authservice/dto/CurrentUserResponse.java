package pfa.dev.authservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurrentUserResponse {
    private String keycloakUserId;
    private String username;
    private List<String> roles;
    private Long employeeId;
}
