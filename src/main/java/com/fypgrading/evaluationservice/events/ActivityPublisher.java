package com.fypgrading.evaluationservice.events;

import com.fypgrading.evaluationservice.evaluation.Assessment;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Publishes "what happened" to the {@code assay.activity} exchange; notification-service turns it into the
 * coordinator's live feed. Best effort: a lost feed item never blocks grading.
 */
@Component
public class ActivityPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityPublisher.class);
    private static final String BINDING = "activity-out-0";

    private final StreamBridge bridge;

    public ActivityPublisher(StreamBridge bridge) {
        this.bridge = bridge;
    }

    public void publish(
            UUID session,
            String kind,
            String actorId,
            String actorName,
            String text,
            Instant at,
            String teamId,
            Assessment assessment) {
        ActivityEvent event = new ActivityEvent(
                UUID.randomUUID().toString(),
                session.toString(),
                kind,
                actorId,
                actorName,
                text,
                at,
                new Link("TEAM", teamId, assessment));
        try {
            if (!bridge.send(BINDING, event)) {
                log.warn("Activity event {} was not accepted by the binder", event.eventId());
            }
        } catch (RuntimeException e) {
            log.warn("Could not publish activity event {}: {}", event.eventId(), e.getMessage());
        }
    }

    public record ActivityEvent(
            String eventId,
            String demoSession,
            String kind,
            String actorId,
            String actorName,
            String text,
            Instant at,
            Link link) {}

    public record Link(String kind, String teamId, Assessment assessment) {}
}
