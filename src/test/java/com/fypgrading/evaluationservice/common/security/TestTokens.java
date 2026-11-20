package com.fypgrading.evaluationservice.common.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real signed tokens for security tests: a throwaway RSA key, its public key served as a JWKS on a local port (as
 * Keycloak serves {@code /protocol/openid-connect/certs}), and RS256 tokens with the claims a test chooses.
 */
public final class TestTokens {

    /** The issuer the {@code test} profile expects ({@code issuer-uri} in application-test.yml). */
    public static final String ISSUER = "http://issuer.test/realms/assay";

    public static final String CLIENT = "assay-spa";

    private static final RSAKey KEY;
    private static final HttpServer JWKS;

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test").generate();
            byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/certs", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            JWKS.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestTokens() {}

    /** For {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}. */
    public static String jwkSetUri() {
        return "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/certs";
    }

    /** A token as Keycloak would issue it to {@code azp}, from {@code issuer}, with these realm roles. */
    public static String token(String issuer, String azp, String... realmRoles) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(UUID.randomUUID().toString())
                    .claim("azp", azp)
                    .claim("preferred_username", "tester")
                    .claim("realm_access", Map.of("roles", List.of(realmRoles)))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(), claims);
            jwt.sign(new RSASSASigner(KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
