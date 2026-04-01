package pfa.dev.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pfa.dev.authservice.event.EmployeeDeletedEvent;
import pfa.dev.authservice.sevice.KeycloakAdminService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeDeletedConsumer {

    private final KeycloakAdminService keycloakAdminService;

    @KafkaListener(
            topics = "employee.deleted",
            groupId = "auth-service",
            properties = {
                    "spring.json.value.default.type=pfa.dev.authservice.event.EmployeeDeletedEvent"
            }
    )
    public void consume(EmployeeDeletedEvent event) {
        log.info("Received employee deletion event for employeeId={} userId={}", event.employeeId(), event.userId());

        boolean deleted = keycloakAdminService.deleteUserIfExists(event.userId());
        if (deleted) {
            log.info("Deleted Keycloak user for employeeId={} userId={}", event.employeeId(), event.userId());
            return;
        }

        log.warn("Keycloak user already absent for employeeId={} userId={}", event.employeeId(), event.userId());
    }
}
