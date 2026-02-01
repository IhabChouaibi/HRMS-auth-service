package pfa.dev.authservice.confi;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    private final KeycloakAdminProperties props;

    public KeycloakAdminConfig(KeycloakAdminProperties props) {
        this.props = props;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm("master")
                .username(props.getAdminUser())
                .password(props.getAdminPassword())
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }
}
