package com.fypgrading.evaluationservice.common.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the seed's relative times into instants when a visitor's copy is created. There is never a fixed
 * clock: "today" (the anchor) is the middle defense day.
 * <ul>
 *   <li>{@code D-110 23:59}: 110 days before the anchor date, 23:59 in Europe/Paris.</li>
 *   <li>{@code N-40m}, {@code N-3h}: 40 minutes / 3 hours before the copy was created.</li>
 * </ul>
 */
public record SeedClock(LocalDate anchor, Instant now) {

    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private static final Pattern DAY = Pattern.compile("^D([+-][0-9]+) ([0-9]{2}:[0-9]{2})$");
    private static final Pattern NOW = Pattern.compile("^N-([0-9]+)([mh])$");

    public static SeedClock startingNow(Clock clock) {
        Instant now = clock.instant();
        return new SeedClock(LocalDate.ofInstant(now, ZONE), now);
    }

    public Instant resolve(String expression) {
        if (expression == null) {
            return null;
        }
        Matcher day = DAY.matcher(expression);
        if (day.matches()) {
            return anchor.plusDays(Long.parseLong(day.group(1)))
                    .atTime(LocalTime.parse(day.group(2)))
                    .atZone(ZONE)
                    .toInstant();
        }
        Matcher ago = NOW.matcher(expression);
        if (ago.matches()) {
            long amount = Long.parseLong(ago.group(1));
            return now.minus("h".equals(ago.group(2)) ? Duration.ofHours(amount) : Duration.ofMinutes(amount));
        }
        throw new IllegalArgumentException("Not a seed time: " + expression);
    }

    /** The next midnight in Europe/Paris, when every demo copy is deleted. */
    public static Instant nextPurge(Instant from) {
        return LocalDate.ofInstant(from, ZONE).plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
