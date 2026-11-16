package com.fypgrading.evaluationservice.common.web;

import java.net.URI;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Every error leaves the service as an RFC 9457 problem with a stable {@code code}. Internals never leak. */
@RestControllerAdvice
public class ProblemHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> api(ApiException ex) {
        ProblemDetail problem = ProblemWriter.problem(ex.status(), ex.code(), ex.getMessage());
        if (!ex.errors().isEmpty()) {
            problem.setProperty("errors", ex.errors());
        }
        ex.properties().forEach(problem::setProperty);
        return ResponseEntity.status(ex.status()).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ProblemWriter.problem(HttpStatus.FORBIDDEN, "forbidden", "Your role can't do this."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> typeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemWriter.problem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "'" + ex.getValue() + "' is not a valid " + ex.getName() + ".");
        problem.setProperty("errors", List.of(new ApiException.FieldProblem(ex.getName(), "invalid value")));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.internalServerError()
                .body(ProblemWriter.problem(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "internal-error",
                        "Something went wrong on our side. Try again in a moment."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiException.FieldProblem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(ProblemHandler::toProblem)
                .toList();
        ProblemDetail problem = ProblemWriter.problem(
                HttpStatus.BAD_REQUEST, "validation-failed", "Some fields are missing or invalid.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiException.FieldProblem> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ApiException.FieldProblem(
                                String.valueOf(r.getMethodParameter().getParameterName()),
                                String.valueOf(e.getDefaultMessage()))))
                .toList();
        ProblemDetail problem =
                ProblemWriter.problem(HttpStatus.BAD_REQUEST, "validation-failed", "Some parameters are invalid.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null
                && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            String code = switch (statusCode.value()) {
                case 400 -> "bad-request";
                case 404 -> "not-found";
                case 405 -> "method-not-allowed";
                case 406, 415 -> "unsupported-media-type";
                default -> "error";
            };
            problem.setType(URI.create(ProblemWriter.TYPE_BASE + code));
            problem.setProperty("code", code);
        }
        return response;
    }

    private static ApiException.FieldProblem toProblem(FieldError error) {
        return new ApiException.FieldProblem(error.getField(), String.valueOf(error.getDefaultMessage()));
    }
}
