package topicmanagement.service;

import java.util.concurrent.atomic.AtomicLong;

public class DbHealthTracker {
    private static final AtomicLong lastFailureTimestamp = new AtomicLong(0);
    private static final long COOLDOWN_MS = 60_000; // 60 seconds cooldown before retrying DB connection

    public static boolean isDbAvailable() {
        long lastFail = lastFailureTimestamp.get();
        if (lastFail == 0) {
            return true;
        }
        // Allow probe retry after COOLDOWN_MS
        return (System.currentTimeMillis() - lastFail) > COOLDOWN_MS;
    }

    public static void recordFailure(Throwable e) {
        lastFailureTimestamp.set(System.currentTimeMillis());
    }

    public static void recordSuccess() {
        lastFailureTimestamp.set(0);
    }
}
