package com.shop.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple sales forecaster: blends weekday seasonality (same weekday over the
 * recent window) with the recent 14-day moving average.
 */
public class SalesForecaster {

    public static class ForecastDay {
        public final LocalDate date;
        public final double value;

        ForecastDay(LocalDate date, double value) {
            this.date = date;
            this.value = value;
        }
    }

    private SalesForecaster() {
    }

    public static List<ForecastDay> forecastNextDays(Map<String, Double> history, int days) {
        LocalDate today = LocalDate.now();

        double recentSum = 0;
        int recentCount = 0;
        for (Map.Entry<String, Double> e : history.entrySet()) {
            LocalDate d = parse(e.getKey());
            if (d != null && d.isAfter(today.minusDays(15))) {
                recentSum += e.getValue();
                recentCount++;
            }
        }
        double recentAvg = recentCount > 0 ? recentSum / recentCount : 0;

        List<ForecastDay> result = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            LocalDate target = today.plusDays(i);
            List<Double> sameWeekday = new ArrayList<>();
            for (Map.Entry<String, Double> e : history.entrySet()) {
                LocalDate d = parse(e.getKey());
                if (d != null && !d.isAfter(today) && d.getDayOfWeek() == target.getDayOfWeek()) {
                    sameWeekday.add(e.getValue());
                }
            }
            double weekdayAvg = sameWeekday.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double forecast = 0.7 * weekdayAvg + 0.3 * recentAvg;
            result.add(new ForecastDay(target, Math.max(0, Math.round(forecast * 100.0) / 100.0)));
        }
        return result;
    }

    private static LocalDate parse(String iso) {
        try {
            return LocalDate.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }
}
