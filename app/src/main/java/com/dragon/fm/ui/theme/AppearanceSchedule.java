package com.dragon.fm.ui.theme;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure Java appearance scheduling, including offline NOAA solar approximation. */
public final class AppearanceSchedule {
    public enum Mode { SYSTEM, MANUAL, SCHEDULE, SUNRISE_SUNSET }

    private static final double DEGREES = Math.PI / 180.0;

    public static boolean isDark(Mode mode, boolean systemDark, boolean manualDark,
                                 long epochMillis, TimeZone zone,
                                 int dayStartMinutes, int nightStartMinutes,
                                 double latitude, double longitude) {
        if (mode == null) throw new IllegalArgumentException("mode == null");
        if (zone == null) throw new IllegalArgumentException("zone == null");
        if (dayStartMinutes < 0 || dayStartMinutes > 1439
                || nightStartMinutes < 0 || nightStartMinutes > 1439) {
            throw new IllegalArgumentException("minutes must be in [0, 1439]");
        }
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)
                || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be in [-90, 90]");
        }
        if (Double.isNaN(longitude) || Double.isInfinite(longitude)
                || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude must be in [-180, 180]");
        }

        switch (mode) {
            case SYSTEM:
                return systemDark;
            case MANUAL:
                return manualDark;
            case SCHEDULE:
                return scheduledDark(epochMillis, zone, dayStartMinutes, nightStartMinutes);
            case SUNRISE_SUNSET:
                return solarDark(epochMillis, zone, latitude, longitude);
            default:
                throw new IllegalArgumentException("unknown mode: " + mode);
        }
    }

    private static boolean scheduledDark(long epochMillis, TimeZone zone,
                                         int dayStart, int nightStart) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTimeInMillis(epochMillis);
        int minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (dayStart == nightStart) return true;
        if (dayStart < nightStart) return minute < dayStart || minute >= nightStart;
        return minute >= nightStart && minute < dayStart;
    }

    private static boolean solarDark(long epochMillis, TimeZone zone,
                                     double latitude, double longitude) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTimeInMillis(epochMillis);
        double hour = calendar.get(Calendar.HOUR_OF_DAY)
                + (calendar.get(Calendar.MINUTE)
                + (calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000.0) / 60.0)
                / 60.0;

        // NOAA's fractional-year approximation; the zenith cutoff includes refraction and solar radius.
        double gamma = 2.0 * Math.PI / 365.0
                * (calendar.get(Calendar.DAY_OF_YEAR) - 1 + (hour - 12.0) / 24.0);
        double equationOfTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2.0 * gamma)
                - 0.040849 * Math.sin(2.0 * gamma));
        double declination = 0.006918 - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2.0 * gamma)
                + 0.000907 * Math.sin(2.0 * gamma) - 0.002697 * Math.cos(3.0 * gamma)
                + 0.00148 * Math.sin(3.0 * gamma);

        double zoneHours = zone.getOffset(epochMillis) / 3600000.0;
        double solarMinutes = (hour * 60.0 + equationOfTime
                + 4.0 * longitude - 60.0 * zoneHours) % 1440.0;
        if (solarMinutes < 0.0) solarMinutes += 1440.0;
        double hourAngle = (solarMinutes / 4.0 - 180.0) * DEGREES;
        double latitudeRadians = latitude * DEGREES;
        double zenithCosine = Math.sin(latitudeRadians) * Math.sin(declination)
                + Math.cos(latitudeRadians) * Math.cos(declination) * Math.cos(hourAngle);
        return zenithCosine < Math.cos(90.833 * DEGREES);
    }

    private AppearanceSchedule() {
    }
}
