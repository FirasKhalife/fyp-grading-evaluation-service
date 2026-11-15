package com.fypgrading.evaluationservice.common.demo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Ids of seeded rows. Each service derives them from the demo session and the seed key
 * (e.g. {@code team:alpha}), so the services agree on ids without calling each other.
 */
public final class SeedIds {

    private SeedIds() {
    }

    public static UUID of(UUID session, String key) {
        return UUID.nameUUIDFromBytes((session + "/" + key).getBytes(StandardCharsets.UTF_8));
    }
}
