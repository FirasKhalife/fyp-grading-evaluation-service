package com.fypgrading.evaluationservice.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fypgrading.evaluationservice.common.web.ApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Review M3: a refusal from another service keeps its own code and detail, and never becomes a broken 500. */
class RemoteTest {

    private static ApiException failWith(HttpStatus status, String body) {
        return catchThrowableOfType(
                ApiException.class,
                () -> Remote.call("admin service", () -> {
                    throw HttpClientErrorException.create(
                            status,
                            status.getReasonPhrase(),
                            new HttpHeaders(),
                            body.getBytes(StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8);
                }));
    }

    @Test
    void aDownstreamProblemKeepsItsCodeAndDetail() {
        ApiException e = failWith(
                HttpStatus.CONFLICT,
                "{\"type\":\"https://assay.dev/problems/assessment-final\",\"status\":409,"
                        + "\"code\":\"assessment-final\",\"detail\":\"The Final report grade is already final.\"}");
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.code()).isEqualTo("assessment-final");
        assertThat(e.getMessage()).isEqualTo("The Final report grade is already final.");
    }

    @Test
    void anUnreadableRefusalGetsAValidSlugCode() {
        ApiException e = failWith(HttpStatus.UNPROCESSABLE_CONTENT, "<html>oops</html>");
        assertThat(e.code()).isEqualTo("rejected-by-admin-service").doesNotContain(" ");
        assertThat(e.getMessage()).isEqualTo("The admin service refused the change.");
    }

    @Test
    void aCodeThatIsNotASlugIsNotPassedOn() {
        ApiException e = failWith(HttpStatus.CONFLICT, "{\"code\":\"bad code with spaces\",\"detail\":\"x\"}");
        assertThat(e.code()).isEqualTo("rejected-by-admin-service");
    }

    @Test
    void anUnreachableServiceIsA503() {
        ApiException e = catchThrowableOfType(
                ApiException.class,
                () -> Remote.call("admin service", () -> {
                    throw new ResourceAccessException("connection refused");
                }));
        assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
