package pfa.dev.authservice.confi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakAdminPropertiesTest {

    @Test
    void helperMethodsShouldBuildUrisAndSecretFlag() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setServerUrl("http://localhost:8080");
        properties.setRealm("hrms");
        properties.setClientSecret("secret-value");

        assertEquals(
                "http://localhost:8080/realms/hrms/protocol/openid-connect/token",
                properties.getTokenUri()
        );
        assertEquals(
                "http://localhost:8080/realms/hrms/protocol/openid-connect/logout",
                properties.getLogoutUri()
        );
        assertTrue(properties.hasClientSecret());
    }

    @Test
    void hasClientSecretShouldReturnFalseForBlankSecret() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setClientSecret("   ");

        assertFalse(properties.hasClientSecret());
    }
}
