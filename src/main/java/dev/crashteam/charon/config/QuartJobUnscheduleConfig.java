package dev.crashteam.charon.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartJobUnscheduleConfig {

    private final Scheduler scheduler;

    @PostConstruct
    public void init() {
        try {
            if (scheduler.checkExists(TriggerKey.triggerKey("balance_payment_trigger"))) {
                scheduler.unscheduleJob(TriggerKey.triggerKey("balance_payment_trigger"));
            }
            if (scheduler.checkExists(TriggerKey.triggerKey("purchase_service_trigger"))) {
                scheduler.unscheduleJob(TriggerKey.triggerKey("purchase_service_trigger"));
            }
        } catch (Exception e) {
            log.error("Exception while deleting old triggers - ", e);
        }
    }
}
