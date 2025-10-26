package com.morawski.dev.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Scheduler and async processing configuration for BrandPulse application.
 *
 * Enables:
 * 1. Scheduled tasks (@Scheduled) - CRON jobs for automated review syncing
 * 2. Async methods (@Async) - Background processing for long-running operations
 *
 * API Plan Section 15.3: Async Processing
 *
 * Background Jobs:
 * - Daily CRON sync: 3:00 AM CET (PRD:29)
 * - Weekly email reports: Sunday 6:00 AM CET (PRD:54)
 * - Initial 90-day import (US-003)
 * - Dashboard aggregate recalculation
 * - AI summary generation
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {

    /**
     * Task scheduler for CRON jobs.
     *
     * Used by:
     * - Daily review sync (3:00 AM CET)
     * - Weekly email reports (Sunday 6:00 AM CET)
     *
     * Pool size: 2 threads (configured in application.properties)
     *
     * @return ThreadPoolTaskScheduler for scheduled tasks
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        log.info("Configuring task scheduler for CRON jobs");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("cron-scheduler-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRejectedExecutionHandler((r, executor) ->
            log.error("Scheduled task rejected: {}", r.toString())
        );

        scheduler.initialize();

        log.info("Task scheduler initialized with pool size: {}", scheduler.getPoolSize());

        return scheduler;
    }

    /**
     * Async task executor for background operations.
     *
     * Used by:
     * - Initial 90-day review import (US-003)
     * - Dashboard aggregate recalculation
     * - AI summary generation
     * - Manual sync operations (US-008)
     *
     * Configuration (from application.properties):
     * - Core pool size: 4
     * - Max pool size: 8
     * - Queue capacity: 100
     *
     * API Plan Section 15.3: Background Jobs
     *
     * @return Executor for @Async methods
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        log.info("Configuring async task executor for background jobs");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler((r, executor1) ->
            log.error("Async task rejected: {}. Queue capacity reached.", r.toString())
        );

        executor.initialize();

        log.info("Async executor initialized - Core: {}, Max: {}, Queue: {}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity()
        );

        return executor;
    }

    /**
     * Dedicated executor for sync jobs to prevent blocking other async operations.
     *
     * Used specifically for:
     * - Review fetching from external APIs (Google, Facebook, Trustpilot)
     * - Initial 90-day import operations
     * - Manual and scheduled sync jobs
     *
     * Separate pool ensures sync operations don't block critical background tasks
     * like dashboard recalculation or AI summary generation.
     *
     * @return Executor for sync operations
     */
    @Bean(name = "syncJobExecutor")
    public Executor syncJobExecutor() {
        log.info("Configuring dedicated executor for sync jobs");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sync-job-");
        executor.setAwaitTerminationSeconds(60); // Longer timeout for external API calls
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler((r, executor1) ->
            log.error("Sync job rejected: {}. Too many concurrent sync operations.", r.toString())
        );

        executor.initialize();

        log.info("Sync job executor initialized - Core: {}, Max: {}, Queue: {}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity()
        );

        return executor;
    }
}