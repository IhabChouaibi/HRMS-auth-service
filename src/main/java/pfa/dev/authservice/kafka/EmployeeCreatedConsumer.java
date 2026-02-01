package pfa.dev.authservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pfa.dev.authservice.event.EmployeeCreatedEvent;
import pfa.dev.authservice.dto.SignupRequest;
import pfa.dev.authservice.sevice.KeycloakAdminService;

@Service
@RequiredArgsConstructor
public class EmployeeCreatedConsumer {

    private final KeycloakAdminService keycloakAdminService;
    @KafkaListener(
            topics = "employee.created",
            groupId = "auth-service"
    )
    public void consume(EmployeeCreatedEvent event) {

        String username =
                capitalize(event.firstName()) +
                        capitalize(event.lastName());

        SignupRequest signupRequest = new SignupRequest(
                username,
                event.email(),
                event.password(),
                event.firstName(),
                event.lastName()
        );

        keycloakAdminService.createUserIfNotExists(signupRequest);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase()
                + value.substring(1).toLowerCase();
    }
}
