package pfa.dev.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter();

    @Test
    void convertShouldMergeScopeAndRealmRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-user-1")
                .claim("preferred_username", "alice")
                .claim("scope", "profile email")
                .claim("realm_access", Map.of("roles", java.util.List.of("HR", "ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals("alice", authentication.getName());
        assertTrue(authorities.contains("SCOPE_profile"));
        assertTrue(authorities.contains("SCOPE_email"));
        assertTrue(authorities.contains("ROLE_HR"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
    }

    @Test
    void convertShouldHandleMissingRealmAccessClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-user-2")
                .claim("preferred_username", "bob")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertEquals("bob", authentication.getName());
        assertTrue(authentication.getAuthorities().isEmpty());
    }
}
