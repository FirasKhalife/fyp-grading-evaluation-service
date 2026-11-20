package com.fypgrading.evaluationservice.common.security;

import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Accepts only tokens that Keycloak issued to one of our own clients ({@code azp}, "authorized party"): today the
 * single-page app, {@code assay-spa}. Without it, a token any realm user gets from another client of the realm
 * (for example Keycloak's built-in {@code admin-cli} password grant) would be accepted too. The issuer is checked
 * by Spring Boot ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri}); Boot picks this validator up as a
 * bean and adds it to the same chain.
 *
 * <p>{@code azp} rather than {@code aud}: Keycloak puts only {@code account} in {@code aud} unless the realm gets an
 * audience mapper, and the realm is imported once, so existing installs would need it added by hand. Every
 * Keycloak access token carries {@code azp}.
 */
public final class AuthorizedParty implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error WRONG_CLIENT =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token was issued to another client.", null);

    private final Set<String> allowed;

    public AuthorizedParty(List<String> allowed) {
        this.allowed = Set.copyOf(allowed);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String azp = jwt.getClaimAsString("azp");
        return azp != null && allowed.contains(azp)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(WRONG_CLIENT);
    }
}
