package pfa.dev.authservice.event;

public record EmployeeDeletedEvent(
        Long employeeId,
        String userId,
        String email
) {
}
