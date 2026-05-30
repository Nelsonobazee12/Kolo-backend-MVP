
package com.kolo.kolo_backend.fines.service

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.stereotype.Component

@Component
class FineEngineJob(
    private val fineService: FineService
) : Job {

    override fun execute(context: JobExecutionContext) {
        fineService.processDailyFines()
    }
}