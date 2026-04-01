package pfa.dev.authservice.util;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;
import pfa.dev.authservice.exception.AuthException;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenParser {

    public ParsedAccessToken parse(String accessToken) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(accessToken).getJWTClaimsSet();
            return new ParsedAccessToken(
                    claims.getSubject(),
                    claims.getStringClaim("preferred_username"),
                    extractRoles(claims)
            );
        } catch (ParseException exception) {
            throw new AuthException("Unable to parse access token", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(JWTClaimsSet claims) {
        Object realmAccessClaim = claims.getClaim("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return Collections.emptyList();
        }

        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof List<?> roles)) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    public record ParsedAccessToken(String subject, String username, List<String> roles) {
    }
}
