package pfa.dev.authservice.confi;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakAdminProperties {

    @NotBlank
    private String serverUrl;

    @NotBlank
    private String realm;

    @NotBlank
    private String adminUser;

    @NotBlank
    private String adminPassword;

    private String clientId;
    private String clientSecret;

}