package dev.crashteam.charon.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
public class CurrencyCacheEvictJob implements Job {
    @Override
    @CacheEvict(value="currency", allEntries=true)
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Cleaning currency cache");
    }
}
