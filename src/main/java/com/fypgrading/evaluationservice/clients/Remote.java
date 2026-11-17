package com.fypgrading.evaluationservice.clients;

import com.fypgrading.evaluationservice.common.web.ApiException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns another service's failure into a problem the visitor can act on. A business refusal (409, 422) keeps the
 * other service's own {@code code} and {@code detail}, so the reviewer sees "this grade is already final" rather
 * than a generic error (review M3).
 */
final class Remote {

    private static final Logger log = LoggerFactory.getLogger(Remote.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern CODE = Pattern.compile("[a-z0-9-]{1,64}");

    private Remote() {}

    static <T> T call(String service, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            Problem downstream = Problem.parse(e.getResponseBodyAsString());
            if (status == HttpStatus.NOT_FOUND) {
                throw ApiException.notFound("The team or assessment");
            }
            if (status == HttpStatus.FORBIDDEN) {
                throw ApiException.forbidden(
                        downstream.codeOr("not-assigned"),
                        downstream.detailOr("You're not a reviewer with this role on this team."));
            }
            if (status == HttpStatus.CONFLICT || status == HttpStatus.UNPROCESSABLE_CONTENT) {
                throw new ApiException(
                        status,
                        downstream.codeOr("rejected-by-" + service.replace(' ', '-')),
                        downstream.detailOr("The " + service + " refused the change."));
            }
            log.warn("{} answered {}", service, e.getStatusCode());
            throw ApiException.unavailable("The " + service + " refused the request. Try again in a minute.");
        } catch (RestClientException | IllegalStateException e) {
            log.warn("{} unreachable: {}", service, e.getMessage());
            throw ApiException.unavailable("The demo is starting up, try again in a minute.");
        }
    }

    /** The parts of an RFC 9457 body worth passing on; anything malformed is ignored. */
    record Problem(String code, String detail) {

        static Problem parse(String body) {
            try {
                JsonNode node = JSON.readTree(body);
                String code = node.path("code").asString(null);
                String detail = node.path("detail").asString(null);
                return new Problem(code != null && CODE.matcher(code).matches() ? code : null, detail);
            } catch (RuntimeException notJson) {
                return new Problem(null, null);
            }
        }

        String codeOr(String fallback) {
            return code != null ? code : fallback;
        }

        String detailOr(String fallback) {
            return detail != null && !detail.isBlank() && detail.length() <= 300 ? detail : fallback;
        }
    }
}
