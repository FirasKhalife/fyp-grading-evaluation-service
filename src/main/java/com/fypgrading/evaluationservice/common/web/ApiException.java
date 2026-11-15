package com.fypgrading.evaluationservice.common.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * A business-rule or request failure that becomes an RFC 9457 problem response. {@code code} is the stable,
 * machine-readable reason the UI switches on; the message is a sentence the UI can show.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldProblem> errors;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, List.of());
    }

    public ApiException(HttpStatus status, String code, String detail, List<FieldProblem> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "not-found", what + " was not found.");
    }

    public static ApiException badRequest(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }

    public static ApiException forbidden(String code, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, code, detail);
    }

    public static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }

    public static ApiException unprocessable(String code, String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, detail);
    }

    public static ApiException unavailable(String detail) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "service-unavailable", detail);
    }

    public ApiException with(String property, Object value) {
        properties.put(property, value);
        return this;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<FieldProblem> errors() {
        return errors;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public record FieldProblem(String field, String message) {
    }
}
