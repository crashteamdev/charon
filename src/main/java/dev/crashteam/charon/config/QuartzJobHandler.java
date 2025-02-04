package dev.crashteam.charon.config;

import dev.crashteam.charon.job.*;
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

    @Value("${app.scheduler.balance-deposit.cron}")
    private String balanceDeposit;

    @Value("${app.scheduler.purchase-service.cron}")
    private String purchaseService;

    @Value("${app.scheduler.recurrent-payment.cron}")
    private String recurrentPayment;

    @Bean("recurrentPaymentTrigger")
    public CronTriggerFactoryBean recurrentPaymentTrigger(@Qualifier("recurrentPaymentJobDetail") JobDetail jobDetail) {
        return QuartzBeanCreatorConfig.cronTriggerFactoryBean(jobDetail, recurrentPayment,
                "recurrent_payment_trigger");
    }

    @Bean(name = "recurrentPaymentJobDetail")
    public JobDetailFactoryBean recurrentPaymentJob() {
        return QuartzBeanCreatorConfig.jobDetail(RecurrentPaymentJob.class,
                "recurrent_payment_job");
    }

    @Bean("balancePaymentTrigger")
    public CronTriggerFactoryBean balancePaymentTrigger(@Qualifier("balancePaymentJobDetail") JobDetail jobDetail) {
        return QuartzBeanCreatorConfig.cronTriggerFactoryBean(jobDetail, balanceDeposit,
                "balance_payment_trigger");
    }

    @Bean(name = "balancePaymentJobDetail")
    public JobDetailFactoryBean balancePaymentJob() {
        return QuartzBeanCreatorConfig.jobDetail(BalancePaymentJob.class,
                "balance_payment_job");
    }

    @Bean("purchaseServiceTrigger")
    public CronTriggerFactoryBean purchaseServiceTrigger(@Qualifier("purchaseServiceJobDetail") JobDetail jobDetail) {
        return QuartzBeanCreatorConfig.cronTriggerFactoryBean(jobDetail, purchaseService,
                "purchase_service_trigger");
    }

    @Bean(name = "purchaseServiceJobDetail")
    public JobDetailFactoryBean purchaseServiceJob() {
        return QuartzBeanCreatorConfig.jobDetail(PurchaseServiceJob.class,
                "purchase_service_job");
    }

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