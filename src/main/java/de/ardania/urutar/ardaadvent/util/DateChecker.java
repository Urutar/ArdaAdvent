package de.ardania.urutar.ardaadvent.util;

import java.util.Calendar;

public class DateChecker {

    public final static boolean DEBUG = false;
    public final static int DEBUG_DAY = 24;

    public static boolean isDecember() {
        return Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER || DEBUG;
    }

    public static int getDayOfMonth() {
        if (DEBUG) {
            return Math.max(1, DEBUG_DAY);
        }
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isNewCalendarTime() {
        return isDecember() && getDayOfMonth() <= 24;
    }

    public static boolean isLateOpenTime() {
        return isDecember() && getDayOfMonth() <= 31;
    }
}
