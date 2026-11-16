package com.fypgrading.evaluationservice.common.demo;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The visitor's demo session: every row and document is scoped to it, so two visitors never see each
 * other's changes. Bound to the current thread by {@link DemoSessionFilter} for HTTP requests, and by
 * {@link #callAs} for message consumers and jobs.
 */
public final class DemoSession {

    public static final String HEADER = "X-Demo-Session";

    private static final Pattern UUID_V4 =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private DemoSession() {}

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No demo session is bound to this thread");
        }
        return id;
    }

    /** Binds the session until the returned binding is closed (use with try-with-resources). */
    public static Binding bind(UUID session) {
        UUID previous = CURRENT.get();
        CURRENT.set(session);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public static <T> T callAs(UUID session, Supplier<T> work) {
        try (Binding ignored = bind(session)) {
            return work.get();
        }
    }

    public static void runAs(UUID session, Runnable work) {
        callAs(session, () -> {
            work.run();
            return null;
        });
    }

    /** Accepts only a random (version 4) UUID, the format the browser generates. */
    public static Optional<UUID> parse(String raw) {
        if (raw == null || !UUID_V4.matcher(raw.trim()).matches()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(raw.trim()));
    }

    @FunctionalInterface
    public interface Binding extends AutoCloseable {
        @Override
        void close();
    }
}
