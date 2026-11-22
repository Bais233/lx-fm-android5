package com.dragon.fm.player;

/** Monotonic sleep-timer arithmetic; kept Android-free for the tiny JVM check. */
final class SleepTimerClock {
    private SleepTimerClock() {}

    static long deadlineAfterMinutes(int minutes, long nowMs) {
        if (minutes <= 0) throw new IllegalArgumentException("minutes must be positive");
        return nowMs + minutes * 60_000L;
    }

    static int remainingSeconds(long deadlineMs, long nowMs) {
        long remainingMs = deadlineMs - nowMs;
        if (remainingMs <= 0L) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (remainingMs + 999L) / 1000L);
    }
}
