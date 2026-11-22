package com.dragon.fm.ui.theme;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Standalone check: compile with the main source, no Android or test library needed. */
public final class AppearanceScheduleCheck {
    private static final TimeZone SHANGHAI = TimeZone.getTimeZone("Asia/Shanghai");
    private static int assertions;

    public static void main(String[] args) {
        checkSystemAndManual();
        checkScheduleAndMidnight();
        checkShanghaiSolarTime();
        checkPolarDayAndNight();
        checkInvalidArguments();
        System.out.println("PASS: " + assertions + " assertions");
        System.out.println("Covered: SYSTEM/MANUAL, cross-midnight schedule, Shanghai solar day/night, polar day/night, invalid arguments");
    }

    private static void checkSystemAndManual() {
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SYSTEM, true, false,
                local(2024, Calendar.JUNE, 1, 12), SHANGHAI, 360, 1200, 31.23, 121.47), "SYSTEM follows dark system");
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SYSTEM, false, true,
                local(2024, Calendar.JUNE, 1, 12), SHANGHAI, 360, 1200, 31.23, 121.47), "SYSTEM follows light system");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.MANUAL, false, true,
                local(2024, Calendar.JUNE, 1, 12), SHANGHAI, 360, 1200, 31.23, 121.47), "MANUAL dark");
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.MANUAL, true, false,
                local(2024, Calendar.JUNE, 1, 12), SHANGHAI, 360, 1200, 31.23, 121.47), "MANUAL light");
    }

    private static void checkScheduleAndMidnight() {
        long noon = local(2024, Calendar.JUNE, 1, 12);
        long evening = local(2024, Calendar.JUNE, 1, 20);
        long beforeMidnight = local(2024, Calendar.JUNE, 1, 23);
        long afterMidnight = local(2024, Calendar.JUNE, 2, 2);
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                noon, SHANGHAI, 360, 1200, 31.23, 121.47), "SCHEDULE daytime");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                evening, SHANGHAI, 360, 1200, 31.23, 121.47), "SCHEDULE night");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                beforeMidnight, SHANGHAI, 360, 1320, 31.23, 121.47), "cross-midnight before midnight");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                afterMidnight, SHANGHAI, 360, 1320, 31.23, 121.47), "cross-midnight after midnight");
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                noon, SHANGHAI, 360, 1320, 31.23, 121.47), "cross-midnight day remains light");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SCHEDULE, false, false,
                noon, SHANGHAI, 360, 360, 31.23, 121.47), "equal boundaries mean always dark");
    }

    private static void checkShanghaiSolarTime() {
        long summerDay = local(2024, Calendar.JUNE, 1, 12);
        long summerNight = local(2024, Calendar.JUNE, 1, 23);
        long winterDay = local(2024, Calendar.DECEMBER, 1, 12);
        long winterNight = local(2024, Calendar.DECEMBER, 1, 23);
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                summerDay, SHANGHAI, 360, 1200, 31.2304, 121.4737), "Shanghai summer noon is light");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                summerNight, SHANGHAI, 360, 1200, 31.2304, 121.4737), "Shanghai summer night is dark");
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                winterDay, SHANGHAI, 360, 1200, 31.2304, 121.4737), "Shanghai winter noon is light");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                winterNight, SHANGHAI, 360, 1200, 31.2304, 121.4737), "Shanghai winter night is dark");
    }

    private static void checkPolarDayAndNight() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        check(!AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                local(2024, Calendar.JUNE, 21, 0, utc), utc, 360, 1200, 80, 0), "polar day is light at midnight");
        check(AppearanceSchedule.isDark(AppearanceSchedule.Mode.SUNRISE_SUNSET, false, false,
                local(2024, Calendar.DECEMBER, 21, 12, utc), utc, 360, 1200, 80, 0), "polar night is dark at noon");
    }

    private static void checkInvalidArguments() {
        long now = local(2024, Calendar.JUNE, 1, 12);
        for (AppearanceSchedule.Mode mode : AppearanceSchedule.Mode.values()) {
            rejects(mode, now, SHANGHAI, -1, 1200, 31.23, 121.47, "day minutes below zero");
            rejects(mode, now, SHANGHAI, 1440, 1200, 31.23, 121.47, "day minutes above range");
            rejects(mode, now, SHANGHAI, 360, -1, 31.23, 121.47, "night minutes below zero");
            rejects(mode, now, SHANGHAI, 360, 1440, 31.23, 121.47, "night minutes above range");
        }
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, -90.01, 121.47, "latitude too low");
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, 90.01, 121.47, "latitude too high");
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, Double.NaN, 121.47, "NaN latitude");
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, 31.23, -180.01, "longitude too low");
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, 31.23, 180.01, "longitude too high");
        rejects(AppearanceSchedule.Mode.SUNRISE_SUNSET, now, SHANGHAI, 360, 1200, 31.23, Double.POSITIVE_INFINITY, "infinite longitude");
        rejects(AppearanceSchedule.Mode.SYSTEM, now, null, 360, 1200, 31.23, 121.47, "null zone");
        boolean rejected = false;
        try {
            AppearanceSchedule.isDark(null, false, false, now, SHANGHAI, 360, 1200, 31.23, 121.47);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "null mode");
    }

    private static void rejects(AppearanceSchedule.Mode mode, long time, TimeZone zone,
                                int day, int night, double latitude, double longitude, String name) {
        boolean rejected = false;
        try {
            AppearanceSchedule.isDark(mode, false, false, time, zone, day, night, latitude, longitude);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, name);
    }

    private static long local(int year, int month, int day, int hour) {
        return local(year, month, day, hour, SHANGHAI);
    }

    private static long local(int year, int month, int day, int hour, TimeZone zone) {
        Calendar calendar = new GregorianCalendar(zone);
        calendar.clear();
        calendar.set(year, month, day, hour, 0, 0);
        return calendar.getTimeInMillis();
    }

    private static void check(boolean condition, String name) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }

    private AppearanceScheduleCheck() {
    }
}
