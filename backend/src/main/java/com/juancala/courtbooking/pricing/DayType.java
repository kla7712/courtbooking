package com.juancala.courtbooking.pricing;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum DayType {
    WEEKDAY,
    WEEKEND;

    public static DayType of(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) ? WEEKEND : WEEKDAY;
    }
}
