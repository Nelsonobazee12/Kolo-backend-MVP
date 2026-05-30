
package com.kolo.kolo_backend.shared.config

import com.kolo.kolo_backend.fines.service.FineEngineJob
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuartzConfig {

    @Bean
    fun fineEngineJobDetail(): JobDetail {
        return JobBuilder.newJob(FineEngineJob::class.java)
            .withIdentity("fineEngineJob")
            .storeDurably()
            .build()
    }

    @Bean
    fun fineEngineTrigger(fineEngineJobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(fineEngineJobDetail)
            .withIdentity("fineEngineTrigger")
            .withSchedule(
                // Runs every day at midnight
                CronScheduleBuilder.cronSchedule("0 0 0 * * ?")
            )
            .build()
    }
}