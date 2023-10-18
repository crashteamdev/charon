package dev.crashteam.charon.config;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Configuration
public class QuartzBeanCreatorConfig {

    static CronTriggerFactoryBean cronTriggerFactoryBean(JobDetail job, String cron, String triggerName) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(job);
        factoryBean.setCronExpression(cron);
        factoryBean.setStartDelay(0L);
        factoryBean.setName(triggerName);
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factoryBean;
    }

    static JobDetailFactoryBean jobDetail(Class<? extends Job> jobClass, String jobName) {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(jobClass);
        jobDetailFactory.setDurability(true);
        jobDetailFactory.setName(jobName);
        return jobDetailFactory;
    }
}
