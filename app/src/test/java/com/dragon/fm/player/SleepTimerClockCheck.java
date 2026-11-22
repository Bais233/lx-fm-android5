package com.dragon.fm.player;

/** Run with javac/java; no test framework or Android runtime required. */
public final class SleepTimerClockCheck {
    public static void main(String[] args) {
        long deadline = SleepTimerClock.deadlineAfterMinutes(30, 10_000L);
        check(deadline == 1_810_000L, "30-minute deadline");
        check(SleepTimerClock.remainingSeconds(deadline, 10_000L) == 1800, "initial value");
        check(SleepTimerClock.remainingSeconds(deadline, 10_001L) == 1800, "round up partial second");
        check(SleepTimerClock.remainingSeconds(deadline, 1_809_001L) == 1, "last partial second");
        check(SleepTimerClock.remainingSeconds(deadline, 1_810_000L) == 0, "deadline");
        check(SleepTimerClock.remainingSeconds(deadline, 9_810_000L) == 0, "delayed wakeup still expires");
        boolean rejected = false;
        try { SleepTimerClock.deadlineAfterMinutes(0, 0L); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "non-positive duration rejected");
        System.out.println("SleepTimerClockCheck: OK");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
