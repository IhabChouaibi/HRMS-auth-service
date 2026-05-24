package pfa.dev.authservice.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import pfa.dev.authservice.exception.AuthException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenParserTest {

    private final JwtTokenParser parser = new JwtTokenParser();

    @Test
    void parseShouldExtractSubjectUsernameAndRoles() throws Exception {
        String token = signedToken(
                "kc-user-1",
                "alice",
                List.of("HR", "MANAGER")
        );

        JwtTokenParser.ParsedAccessToken parsed = parser.parse(token);

        assertEquals("kc-user-1", parsed.subject());
        assertEquals("alice", parsed.username());
        assertEquals(List.of("HR", "MANAGER"), parsed.roles());
    }

    @Test
    void parseShouldReturnEmptyRolesWhenRealmAccessClaimIsMissing() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("kc-user-2")
                .claim("preferred_username", "bob")
                .issueTime(java.util.Date.from(Instant.now()))
                .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .build();

        String token = sign(claims);

        JwtTokenParser.ParsedAccessToken parsed = parser.parse(token);

        assertEquals("kc-user-2", parsed.subject());
        assertEquals("bob", parsed.username());
        assertTrue(parsed.roles().isEmpty());
    }

    @Test
    void parseShouldThrowAuthExceptionForMalformedToken() {
        AuthException exception = assertThrows(AuthException.class, () -> parser.parse("invalid-token"));

        assertEquals("Unable to parse access token", exception.getMessage());
    }

    private static String signedToken(String subject, String username, List<String> roles) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("preferred_username", username)
                .claim("realm_access", java.util.Map.of("roles", roles))
                .issueTime(java.util.Date.from(Instant.now()))
                .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .build();
        return sign(claims);
    }

    private static String sign(JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner("12345678901234567890123456789012"));
        return jwt.serialize();
    }
}
