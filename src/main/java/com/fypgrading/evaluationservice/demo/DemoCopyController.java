package com.fypgrading.evaluationservice.demo;

import com.fypgrading.evaluationservice.common.demo.DemoSession;
import com.fypgrading.evaluationservice.common.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal: called by admin-service when a visitor's demo copy is created, reset or dropped. The gateway doesn't
 * route {@code /internal/**}; the caller forwards the visitor's token and demo session, and the session in the path
 * must be that same session (review M8), so a token can't touch another visitor's copy.
 */
@RestController
@RequestMapping("/internal/demo-sessions")
public class DemoCopyController {

    private final DemoCopyService copies;
    private final Clock clock;

    public DemoCopyController(DemoCopyService copies, Clock clock) {
        this.copies = copies;
        this.clock = clock;
    }

    /** {@code createdAt}: the copy's creation instant from admin-service, so every service agrees on "today". */
    @PutMapping("/{session}")
    public ResponseEntity<Void> replace(
            @PathVariable UUID session,
            @RequestHeader(DemoSession.HEADER) String header,
            @RequestParam(required = false) Instant createdAt) {
        requireSame(session, header);
        copies.replace(session, createdAt == null ? clock.instant() : createdAt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{session}")
    public ResponseEntity<Void> delete(@PathVariable UUID session, @RequestHeader(DemoSession.HEADER) String header) {
        requireSame(session, header);
        copies.delete(session);
        return ResponseEntity.noContent().build();
    }

    private static void requireSame(UUID session, String header) {
        if (!DemoSession.parse(header).map(session::equals).orElse(false)) {
            throw ApiException.forbidden("not-your-session", "A demo copy can only be changed by its own session.");
        }
    }
}
