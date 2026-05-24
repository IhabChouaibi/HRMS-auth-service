package pfa.dev.authservice.service.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import pfa.dev.authservice.client.EmployeeServiceClient;
import pfa.dev.authservice.confi.KeycloakAdminProperties;
import pfa.dev.authservice.dto.CurrentUserResponse;
import pfa.dev.authservice.dto.EmployeeLookupResponse;
import pfa.dev.authservice.exception.AuthException;
import pfa.dev.authservice.util.JwtTokenParser;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private KeycloakAdminProperties keycloakAdminProperties;

    @Mock
    private JwtTokenParser jwtTokenParser;

    @Mock
    private EmployeeServiceClient employeeServiceClient;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserShouldRejectNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));

        AuthException exception = assertThrows(AuthException.class, () -> authService.getCurrentUser());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    void getCurrentUserShouldReturnStaticEmployeeIdForHrRole() {
        setJwtAuthentication("jwt-token", "alice");
        when(jwtTokenParser.parse("jwt-token"))
                .thenReturn(new JwtTokenParser.ParsedAccessToken("kc-user-1", "alice", List.of("HR")));

        CurrentUserResponse response = authService.getCurrentUser();

        assertEquals("kc-user-1", response.getKeycloakUserId());
        assertEquals("alice", response.getUsername());
        assertEquals(List.of("HR"), response.getRoles());
        assertEquals(1L, response.getEmployeeId());
        verify(employeeServiceClient, never()).getEmployeeByUserId(anyString(), anyString());
    }

    @Test
    void getCurrentUserShouldLoadEmployeeIdFromEmployeeService() {
        setJwtAuthentication("jwt-token", "bob");
        when(jwtTokenParser.parse("jwt-token"))
                .thenReturn(new JwtTokenParser.ParsedAccessToken("kc-user-2", "bob", List.of("EMPLOYEE")));

        EmployeeLookupResponse employee = new EmployeeLookupResponse();
        employee.setId(42L);
        when(employeeServiceClient.getEmployeeByUserId("kc-user-2", "Bearer jwt-token")).thenReturn(employee);

        CurrentUserResponse response = authService.getCurrentUser();

        assertEquals(42L, response.getEmployeeId());
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(employeeServiceClient).getEmployeeByUserId(org.mockito.ArgumentMatchers.eq("kc-user-2"), headerCaptor.capture());
        assertEquals("Bearer jwt-token", headerCaptor.getValue());
    }

    @Test
    void getCurrentUserShouldReturnNullEmployeeIdWhenEmployeeDoesNotExist() {
        setJwtAuthentication("jwt-token", "charlie");
        when(jwtTokenParser.parse("jwt-token"))
                .thenReturn(new JwtTokenParser.ParsedAccessToken("kc-user-3", "charlie", List.of("EMPLOYEE")));
        when(employeeServiceClient.getEmployeeByUserId("kc-user-3", "Bearer jwt-token"))
                .thenThrow(notFoundException());

        CurrentUserResponse response = authService.getCurrentUser();

        assertNull(response.getEmployeeId());
    }

    @Test
    void getCurrentUserShouldWrapEmployeeServiceFailures() {
        setJwtAuthentication("jwt-token", "dora");
        when(jwtTokenParser.parse("jwt-token"))
                .thenReturn(new JwtTokenParser.ParsedAccessToken("kc-user-4", "dora", List.of("EMPLOYEE")));
        when(employeeServiceClient.getEmployeeByUserId("kc-user-4", "Bearer jwt-token"))
                .thenThrow(serverErrorException());

        AuthException exception = assertThrows(AuthException.class, () -> authService.getCurrentUser());

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("Unable to retrieve employee information", exception.getMessage());
    }

    private static void setJwtAuthentication(String tokenValue, String username) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject("subject")
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static FeignException.NotFound notFoundException() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(baseRequest())
                .headers(Map.of())
                .body(new byte[0])
                .build();
        return (FeignException.NotFound) FeignException.errorStatus("getEmployeeByUserId", response);
    }

    private static FeignException serverErrorException() {
        Response response = Response.builder()
                .status(500)
                .reason("Server Error")
                .request(baseRequest())
                .headers(Map.of())
                .body("failure", StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("getEmployeeByUserId", response);
    }

    private static Request baseRequest() {
        return Request.create(
                Request.HttpMethod.GET,
                "http://employee-service/api/employees/by-user-id/test",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
    }
}
