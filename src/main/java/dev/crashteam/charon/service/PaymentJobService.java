package dev.crashteam.charon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJobService {

    private final Scheduler scheduler;

    @Value("${app.payment-check.exponent}")
    private double exponent;

    @Value("${app.payment-check.maxSeconds}")
    private int maxSeconds;

    public void schedulePaymentJob(String paymentId, Class<? extends Job> jobClass, int seconds, String jobName) {
        int secondsToAdd = (int) (seconds * exponent);
        String jobNameFormatted = jobName.formatted(paymentId, secondsToAdd);
        JobKey jobKey = new JobKey(jobNameFormatted);
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .requestRecovery(true)
                .build();
        jobDetail.getJobDataMap().put("payment_id", String.valueOf(paymentId));
        if (secondsToAdd >= maxSeconds) {
            jobNameFormatted = jobName.formatted(paymentId, maxSeconds);
            jobDetail.getJobDataMap().put("seconds", String.valueOf(maxSeconds));
        } else {
            jobDetail.getJobDataMap().put("seconds", String.valueOf(secondsToAdd));
        }

        Date futureDate = DateBuilder.futureDate(secondsToAdd, DateBuilder.IntervalUnit.SECOND);

        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + jobNameFormatted)
                .startAt(futureDate)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
        try {
            scheduler.scheduleJob(jobDetail, simpleTrigger);
        } catch (Exception e) {
            log.error("Failed to schedule job for payment {}", paymentId, e);
        }
    }
}
