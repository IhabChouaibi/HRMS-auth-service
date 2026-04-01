package pfa.dev.authservice.sevice;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pfa.dev.authservice.confi.KeycloakAdminProperties;
import pfa.dev.authservice.dto.CreateUserRequest;
import pfa.dev.authservice.dto.CreateUserResponse;
import pfa.dev.authservice.dto.LoginRequest;
import pfa.dev.authservice.dto.LoginResponse;
import pfa.dev.authservice.dto.SignupRequest;
import pfa.dev.authservice.exception.AuthException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {
    private final KeycloakAdminProperties props;
    private final Keycloak keycloak;

    public LoginResponse login(LoginRequest request) {
        Keycloak keycloakUser = KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getRealm())
                .username(request.getUsername())
                .password(request.getPassword())
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .grantType(OAuth2Constants.PASSWORD)
                .scope("openid profile email")
                .build();

        String accessToken = keycloakUser.tokenManager().getAccessTokenString();
        long expiresIn = keycloakUser.tokenManager().getAccessToken().getExpiresIn();

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(keycloakUser.tokenManager().getAccessToken().getRefreshToken());
        response.setExpiresIn(expiresIn);

        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            response.setUsername(claims.getStringClaim("preferred_username"));

            Map<String, Object> realmAccess = (Map<String, Object>) claims.getClaim("realm_access");
            if (realmAccess != null) {
                response.setRoles((List<String>) realmAccess.get("roles"));
            }
        } catch (Exception exception) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Error decoding token", exception);
        } finally {
            keycloakUser.close();
        }

        return response;
    }

    public void createUserIfNotExists(SignupRequest request) {
        if (userExists(request.getEmail())) {
            return;
        }
        createUserAccount(toCreateUserRequest(request));
    }

    public String createUser(@Valid SignupRequest request) {
        createUserAccount(toCreateUserRequest(request));
        return "Utilisateur cree avec succes !";
    }

    public CreateUserResponse createUserAccount(@Valid CreateUserRequest request) {
        if (userExists(request.getEmail())) {
            throw new AuthException(HttpStatus.CONFLICT, "A Keycloak user already exists with email: " + request.getEmail());
        }
        if (usernameExists(request.getUsername())) {
            throw new AuthException(HttpStatus.CONFLICT, "A Keycloak user already exists with username: " + request.getUsername());
        }

        UserRepresentation user = buildUserRepresentation(request);

        try (Response response = keycloak.realm(props.getRealm()).users().create(user)) {
            if (response.getStatus() != HttpStatus.CREATED.value()) {
                throw new AuthException(HttpStatus.BAD_GATEWAY, "Keycloak user creation failed with status: " + response.getStatus());
            }

            String keycloakUserId = CreatedResponseUtil.getCreatedId(response);
            return CreateUserResponse.builder()
                    .keycloakUserId(keycloakUserId)
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .build();
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to create Keycloak user", exception);
        }
    }

    public UserRepresentation getUserById(String userId) {
        return keycloak.realm(props.getRealm()).users().get(userId).toRepresentation();
    }

    public boolean userExists(String email) {
        return !keycloak.realm(props.getRealm()).users().searchByEmail(email, true).isEmpty();
    }

    public boolean usernameExists(String username) {
        return !keycloak.realm(props.getRealm()).users().searchByUsername(username, true).isEmpty();
    }

    public void sendResetPasswordEmail(String email) {
        UserRepresentation user = findUserByEmail(email);

        try {
            keycloak.realm(props.getRealm())
                    .users()
                    .get(user.getId())
                    .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
        } catch (Exception e) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Erreur SMTP ou Keycloak : " + e.getMessage(), e);
        }
    }

    public void resetPassword(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Email invalide");
        }
        if (password == null || password.length() < 6) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Mot de passe trop court");
        }

        List<UserRepresentation> users = keycloak.realm(props.getRealm())
                .users()
                .search(email, true);

        if (users.isEmpty()) {
            throw new AuthException(HttpStatus.NOT_FOUND, "Utilisateur inexistant : " + email);
        }

        UserRepresentation user = users.get(0);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setTemporary(false);
        cred.setValue(password);

        try {
            keycloak.realm(props.getRealm())
                    .users()
                    .get(user.getId())
                    .resetPassword(cred);
        } catch (Exception e) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Erreur lors du reset du mot de passe : " + e.getMessage(), e);
        }
    }

    public void logout(String email) {
        UserRepresentation user = findUserByEmail(email);

        try {
            keycloak.realm(props.getRealm())
                    .users()
                    .get(user.getId())
                    .logout();
        } catch (Exception e) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Erreur lors du logout : " + e.getMessage(), e);
        }
    }

    public void deleteUser(String userId) {
        keycloak.realm(props.getRealm()).users().delete(userId);
    }

    public boolean deleteUserIfExists(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        try {
            keycloak.realm(props.getRealm())
                    .users()
                    .get(userId)
                    .remove();
            return true;
        } catch (NotFoundException exception) {
            log.warn("Keycloak user not found for deletion: {}", userId);
            return false;
        } catch (Exception exception) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to delete Keycloak user: " + userId, exception);
        }
    }

    private UserRepresentation findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Email invalide");
        }

        List<UserRepresentation> users = keycloak
                .realm(props.getRealm())
                .users()
                .searchByEmail(email.trim(), true);

        return users.stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Utilisateur inexistant : " + email));
    }

    private UserRepresentation buildUserRepresentation(CreateUserRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        return user;
    }

    private CreateUserRequest toCreateUserRequest(SignupRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(request.getUsername());
        createUserRequest.setEmail(request.getEmail());
        createUserRequest.setPassword(request.getPassword());
        createUserRequest.setFirstName(request.getFirstName());
        createUserRequest.setLastName(request.getLastName());
        return createUserRequest;
    }
}
