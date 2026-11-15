package com.fypgrading.evaluationservice.demo;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal: called by admin-service when a visitor's demo copy is created or reset. The gateway doesn't
 * route {@code /internal/**}; the caller forwards the visitor's token.
 */
@RestController
@RequestMapping("/internal/demo-sessions")
public class DemoCopyController {

    private final DemoCopyService copies;

    public DemoCopyController(DemoCopyService copies) {
        this.copies = copies;
    }

    @PutMapping("/{session}")
    public ResponseEntity<Void> replace(@PathVariable UUID session) {
        copies.replace(session);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{session}")
    public ResponseEntity<Void> delete(@PathVariable UUID session) {
        copies.delete(session);
        return ResponseEntity.noContent().build();
    }
}
