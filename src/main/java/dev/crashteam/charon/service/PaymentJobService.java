package dev.crashteam.charon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJobService {

    private final Scheduler scheduler;

    public void schedulePaymentJob(String paymentId, Class<? extends Job> jobClass, int seconds, String jobName) {
        double exponent = 1.8;
        int secondsToAdd = (int) (seconds * exponent);
        JobKey jobKey = new JobKey(jobName.formatted(paymentId, secondsToAdd));
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .requestRecovery(true)
                .build();
        jobDetail.getJobDataMap().put("payment_id", String.valueOf(paymentId));
        jobDetail.getJobDataMap().put("seconds", String.valueOf(secondsToAdd));

        Date futureDate = DateBuilder.futureDate(secondsToAdd, DateBuilder.IntervalUnit.SECOND);

        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + jobName.formatted(paymentId, secondsToAdd))
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
