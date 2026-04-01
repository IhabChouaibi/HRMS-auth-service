package pfa.dev.authservice.dto;

import lombok.Data;

@Data
public class EmployeeLookupResponse {
    private Long id;
    private String userId;
    private String email;
}
