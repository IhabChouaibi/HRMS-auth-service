package pfa.dev.authservice.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pfa.dev.authservice.client.EmployeeServiceClient;
import pfa.dev.authservice.confi.KeycloakAdminProperties;
import pfa.dev.authservice.dto.CurrentUserResponse;
import pfa.dev.authservice.dto.EmployeeLookupResponse;
import pfa.dev.authservice.dto.KeycloakTokenResponse;
import pfa.dev.authservice.dto.LoginRequest;
import pfa.dev.authservice.dto.LoginResponse;
import pfa.dev.authservice.dto.LogoutRequest;
import pfa.dev.authservice.dto.RefreshTokenRequest;
import pfa.dev.authservice.exception.AuthException;
import pfa.dev.authservice.service.AuthService;
import pfa.dev.authservice.util.JwtTokenParser;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakAdminProperties keycloakProperties;
    private final JwtTokenParser jwtTokenParser;
    private final EmployeeServiceClient employeeServiceClient;

    @Override
    public LoginResponse login(LoginRequest request) {
        MultiValueMap<String, String> formData = baseClientFormData();
        formData.add("grant_type", "password");
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());

        KeycloakTokenResponse tokenResponse = requestToken(formData, "Invalid username or password");
        return buildLoginResponse(tokenResponse);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        MultiValueMap<String, String> formData = baseClientFormData();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", request.getRefreshToken());

        KeycloakTokenResponse tokenResponse = requestToken(formData, "Invalid refresh token");
        if (tokenResponse.getRefreshToken() == null || tokenResponse.getRefreshToken().isBlank()) {
            tokenResponse.setRefreshToken(request.getRefreshToken());
        }
        return buildLoginResponse(tokenResponse);
    }

    @Override
    public void logout(LogoutRequest request) {
        MultiValueMap<String, String> formData = baseClientFormData();
        formData.add("refresh_token", request.getRefreshToken());

        webClientBuilder.build()
                .post()
                .uri(keycloakProperties.getLogoutUri())
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode -> HttpStatusCode.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("Keycloak logout failed")
                                .map(body -> new AuthException(HttpStatus.BAD_REQUEST, body))
                )
                .toBodilessEntity()
                .block();
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "No authenticated user found");
        }

        JwtTokenParser.ParsedAccessToken parsedToken =
                jwtTokenParser.parse(jwtAuthenticationToken.getToken().getTokenValue());

        return CurrentUserResponse.builder()
                .keycloakUserId(parsedToken.subject())
                .username(parsedToken.username())
                .roles(parsedToken.roles())
                .employeeId(resolveEmployeeId(
                        parsedToken.subject(),
                        jwtAuthenticationToken.getToken().getTokenValue(),
                        parsedToken.roles()
                ))
                .build();
    }

    private KeycloakTokenResponse requestToken(MultiValueMap<String, String> formData, String unauthorizedMessage) {
        try {
            KeycloakTokenResponse response = webClientBuilder.build()
                    .post()
                    .uri(keycloakProperties.getTokenUri())
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.BAD_REQUEST.value(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty(unauthorizedMessage)
                                    .map(body -> new AuthException(HttpStatus.UNAUTHORIZED, unauthorizedMessage)))
                    .onStatus(HttpStatusCode -> HttpStatusCode.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("Keycloak token endpoint failed")
                                    .map(body -> new AuthException(HttpStatus.BAD_GATEWAY, body)))
                    .bodyToMono(KeycloakTokenResponse.class)
                    .switchIfEmpty(Mono.error(new AuthException(HttpStatus.BAD_GATEWAY, "Empty response from Keycloak")))
                    .block();

            if (response == null || response.getAccessToken() == null || response.getAccessToken().isBlank()) {
                throw new AuthException(HttpStatus.BAD_GATEWAY, "Keycloak did not return an access token");
            }
            return response;
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to reach Keycloak", exception);
        }
    }

    private LoginResponse buildLoginResponse(KeycloakTokenResponse tokenResponse) {
        JwtTokenParser.ParsedAccessToken parsedToken = jwtTokenParser.parse(tokenResponse.getAccessToken());

        return LoginResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresIn(tokenResponse.getExpiresIn())
                .username(parsedToken.username())
                .roles(parsedToken.roles())
                .employeeId(resolveEmployeeId(
                        parsedToken.subject(),
                        tokenResponse.getAccessToken(),
                        parsedToken.roles()
                ))
                .build();
    }

    private Long resolveEmployeeId(String keycloakUserId, String accessToken, List<String> roles) {
        if (roles != null && roles.stream().anyMatch(role -> "HR".equalsIgnoreCase(role))) {
            return 1L;
        }

        try {
            EmployeeLookupResponse employee = employeeServiceClient.getEmployeeByUserId(
                    keycloakUserId,
                    buildAuthorizationHeader(accessToken)
            );
            return employee != null ? employee.getId() : null;
        } catch (FeignException.NotFound exception) {
            return null;
        } catch (FeignException exception) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to retrieve employee information", exception);
        }
    }

    private MultiValueMap<String, String> baseClientFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakProperties.getClientId());

        if (keycloakProperties.hasClientSecret()) {
            formData.add("client_secret", keycloakProperties.getClientSecret());
        }
        return formData;
    }

    private String buildAuthorizationHeader(String accessToken) {
        return "Bearer " + accessToken;
    }
}
