package dev.crashteam.charon.config;

import dev.crashteam.charon.job.CurrencyCacheEvictJob;
import dev.crashteam.charon.job.ExchangeRateCacheEvictJob;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Configuration
public class QuartzJobHandler {

    @Value("${app.scheduler.evict-currency.cron}")
    private String evictCurrency;

    @Value("${app.scheduler.exchange-rate.cron}")
    private String exchangeRate;

    @Bean("currencyCacheEvictTrigger")
    public CronTriggerFactoryBean currencyCacheEvictTrigger(@Qualifier("currencyCacheEvictJob") JobDetail jobDetail) {
        return QuartzBeanCreatorConfig.cronTriggerFactoryBean(jobDetail, evictCurrency,
                "currency_cache_evict_trigger");
    }

    @Bean(name = "currencyCacheEvictJob")
    public JobDetailFactoryBean currencyCacheEvictJob() {
        return QuartzBeanCreatorConfig.jobDetail(CurrencyCacheEvictJob.class,
                "currency_cache_evict_job");
    }

    @Bean("exchangeRateCacheEvictTrigger")
    public CronTriggerFactoryBean exchangeRateCacheEvictTrigger(@Qualifier("exchangeRateCacheEvictJob") JobDetail jobDetail) {
        return QuartzBeanCreatorConfig.cronTriggerFactoryBean(jobDetail, exchangeRate,
                "exchange_rate_cache_evict_trigger");
    }

    @Bean(name = "exchangeRateCacheEvictJob")
    public JobDetailFactoryBean exchangeRateCacheEvictJob() {
        return QuartzBeanCreatorConfig.jobDetail(ExchangeRateCacheEvictJob.class,
                "exchange_rate_cache_evict_job");
    }
}