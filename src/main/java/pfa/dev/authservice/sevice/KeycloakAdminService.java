package pfa.dev.authservice.sevice;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import pfa.dev.authservice.confi.KeycloakAdminProperties;
import pfa.dev.authservice.dto.LoginRequest;
import pfa.dev.authservice.dto.LoginResponse;
import pfa.dev.authservice.dto.SignupRequest;


import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {
    private final KeycloakAdminProperties props;
    private final Keycloak keycloak;
    private final String realmName = "micro-service";


    public LoginResponse login(LoginRequest request) {

        // Créer un Keycloak client pour l’utilisateur
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

        // Obtenir le token
        String accessToken = keycloakUser.tokenManager().getAccessTokenString();
        long expiresIn = keycloakUser.tokenManager().getAccessToken().getExpiresIn();

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(keycloakUser.tokenManager().getAccessToken().getRefreshToken());
        response.setExpiresIn(expiresIn);

        // fermer la connexion Keycloak
        keycloakUser.close();

        return response;
    }
    public void createUserIfNotExists(SignupRequest request) {

        if (userExists(request.getEmail())) {
            return; // ✅ idempotence
        }

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
        credential.setTemporary(false); // 🔥 recommandé
        user.setCredentials(List.of(credential));

        keycloak.realm(realmName).users().create(user);
    }


    public String createUser(@Valid SignupRequest request) {
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

        keycloak.realm(realmName).users().create(user);
        return "Utilisateur créé avec succès !";
    }

    public UserRepresentation getUserById(String userId) {
        return keycloak.realm(realmName).users().get(userId).toRepresentation();
    }
    public boolean userExists(String email) {
        List<UserRepresentation> users = keycloak.realm(realmName).users().searchByEmail(email, true);
        return !users.isEmpty();
    }

    public void sendResetPasswordEmail(String email) {
        UserRepresentation user = findUserByEmail(email);

        try {
            keycloak.realm(realmName)
                    .users()
                    .get(user.getId())
                    .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
        } catch (Exception e) {
            throw new RuntimeException("Erreur SMTP ou Keycloak : " + e.getMessage(), e);
        }
    }
    public void resetPassword(String email, String password) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email invalide");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mot de passe trop court");
        }

        List<UserRepresentation> users =
                keycloak.realm(realmName)
                        .users()
                        .search(email, true);

        if (users.isEmpty()) {
            throw new RuntimeException("Utilisateur inexistant : " + email);
        }

        UserRepresentation user = users.get(0);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setTemporary(false);
        cred.setValue(password);

        try {
            keycloak.realm(realmName)
                    .users()
                    .get(user.getId())
                    .resetPassword(cred);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du reset du mot de passe : " + e.getMessage(), e);
        }
    }
    public void logout(String email) {
        UserRepresentation user = findUserByEmail(email);

        try {
            keycloak.realm(realmName)
                    .users()
                    .get(user.getId())
                    .logout();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du logout : " + e.getMessage(), e);
        }
    }


    private UserRepresentation findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email invalide");
        }

        // On utilise search(email, exact) qui est souvent plus fiable que searchByEmail
        // selon la version de l'adapter Keycloak
        List<UserRepresentation> users = keycloak
                .realm(realmName)
                .users()
                .searchByEmail(email.trim(), true);

        return users.stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Utilisateur inexistant : " + email));
    }
  public  void deleteUser(String userId){
        keycloak.realm(realmName).users().delete(userId);
  }

}

