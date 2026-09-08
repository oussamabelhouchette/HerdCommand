package com.herdcommand.api.domain.animal;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

public final class AnimalAgeFormatter {

    private AnimalAgeFormatter() {}

    public static String display(LocalDate dateOfBirth, String language) {
        if (dateOfBirth == null) {
            return "";
        }
        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) {
            return "";
        }
        Period period = Period.between(dateOfBirth, today);
        boolean arabic = language == null || language.isBlank() || language.toLowerCase(Locale.ROOT).startsWith("ar");
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        if (years == 0 && months == 0) {
            return arabic ? daysLabelAr(Math.max(days, 1)) : days + (Math.max(days, 1) == 1 ? " day" : " days");
        }
        if (years == 0) {
            return arabic ? monthsLabelAr(months) : months + (months == 1 ? " month" : " months");
        }
        if (years == 1 && months >= 6) {
            return arabic ? "سنة ونصف" : "1½ years";
        }
        if (arabic) {
            return yearsLabelAr(years);
        }
        return years + (years == 1 ? " year" : " years");
    }

    private static String daysLabelAr(int days) {
        if (days == 1) {
            return "يوم";
        }
        if (days == 2) {
            return "يومان";
        }
        return toArabicDigits(days) + " أيام";
    }

    private static String monthsLabelAr(int months) {
        if (months == 1) {
            return "شهر";
        }
        if (months == 2) {
            return "شهران";
        }
        return toArabicDigits(months) + " أشهر";
    }

    private static String yearsLabelAr(int years) {
        if (years == 1) {
            return "سنة";
        }
        if (years == 2) {
            return "سنتان";
        }
        return toArabicDigits(years) + " سنوات";
    }

    public static String toArabicDigits(int value) {
        String western = String.valueOf(value);
        StringBuilder builder = new StringBuilder(western.length());
        for (int i = 0; i < western.length(); i++) {
            char ch = western.charAt(i);
            builder.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        }
        return builder.toString();
    }
}
