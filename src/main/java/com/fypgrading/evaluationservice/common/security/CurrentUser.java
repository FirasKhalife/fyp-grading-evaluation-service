package com.fypgrading.evaluationservice.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The signed-in user, always taken from the token (never from a request body or path). */
public record CurrentUser(UUID id, String displayName, Jwt jwt, Authentication authentication) {

    public static CurrentUser require() {
        return find().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public static Optional<CurrentUser> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                    .orElse(jwt.getClaimAsString("preferred_username"));
            return Optional.of(new CurrentUser(UUID.fromString(jwt.getSubject()), name, jwt, auth));
        }
        return Optional.empty();
    }

    public boolean hasRole(String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public boolean isAdmin() {
        return hasRole(Roles.ADMIN);
    }

    public String bearerToken() {
        return jwt.getTokenValue();
    }
}
