package com.project.thuongmaidientu.Security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn đăng nhập sai: tối đa MAX_FAILURES lần / WINDOW cho mỗi khóa (email + IP).
 * Bộ nhớ trong tiến trình (đủ cho 1 instance); nhiều instance cần chuyển sang Redis.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempts(int count, Instant windowStart) { }

    private final ConcurrentHashMap<String, Attempts> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempts a = failures.get(key);
        if (a == null) return false;
        if (a.windowStart().plus(WINDOW).isBefore(Instant.now())) {
            failures.remove(key, a);
            return false;
        }
        return a.count() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        failures.merge(key, new Attempts(1, now), (old, ignored) ->
                old.windowStart().plus(WINDOW).isBefore(now)
                        ? new Attempts(1, now)
                        : new Attempts(old.count() + 1, old.windowStart()));
        if (failures.size() > 10_000) failures.entrySet().removeIf(e -> e.getValue().windowStart().plus(WINDOW).isBefore(now));
    }

    public void reset(String key) {
        failures.remove(key);
    }
}