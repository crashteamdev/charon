package dev.crashteam.charon.util;

import feign.RetryableException;
import org.springframework.http.HttpHeaders;

import java.util.Optional;

public class FeignUtils {

    public static Optional<Long> getSleptForMillis(RetryableException e, long currentTime, long maxPeriod, long period, int attempt, long sleptForMillis) {
        Optional<Long> interval = FeignUtils.getInterval(e, currentTime, maxPeriod, period, attempt);

        interval.ifPresent(value -> {
            try {
                Thread.sleep(value);
            } catch (InterruptedException var5) {
                Thread.currentThread().interrupt();
                throw e;
            }
        });
        return interval.map(value -> value + sleptForMillis);
    }

    private static Optional<Long> getInterval(RetryableException e, long currentTime, long maxPeriod, long period, int attempt) {
        long interval;
        if (e.retryAfter() != null) {
            interval = e.retryAfter().getTime() - currentTime;
            if (interval > maxPeriod) {
                interval = maxPeriod;
            }

            if (interval < 0L) {
                return Optional.empty();
            } else {
                return Optional.of(interval);
            }
        } else {
            return Optional.of(nextMaxInterval(maxPeriod, period, attempt));
        }
    }

    private static long nextMaxInterval(long maxPeriod, long period, long attempt) {
        long interval = (long)((double)period * Math.pow(1.5D, (double)(attempt - 1)));
        return Math.min(interval, maxPeriod);
    }

}
