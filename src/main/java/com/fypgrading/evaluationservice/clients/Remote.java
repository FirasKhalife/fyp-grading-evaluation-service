package com.fypgrading.evaluationservice.clients;

import com.fypgrading.evaluationservice.common.web.ApiException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/** Turns another service's failure into a problem the visitor can act on. */
final class Remote {

    private static final Logger log = LoggerFactory.getLogger(Remote.class);

    private Remote() {
    }

    static <T> T call(String service, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw ApiException.notFound("The team or assessment");
            }
            if (status == HttpStatus.FORBIDDEN) {
                throw ApiException.forbidden("not-assigned", "You're not a reviewer with this role on this team.");
            }
            if (status == HttpStatus.CONFLICT || status == HttpStatus.UNPROCESSABLE_CONTENT) {
                throw new ApiException(status, "rejected-by-" + service, e.getResponseBodyAsString());
            }
            log.warn("{} answered {}", service, e.getStatusCode());
            throw ApiException.unavailable("The " + service + " refused the request. Try again in a minute.");
        } catch (RestClientException | IllegalStateException e) {
            log.warn("{} unreachable: {}", service, e.getMessage());
            throw ApiException.unavailable("The demo is starting up, try again in a minute.");
        }
    }
}
