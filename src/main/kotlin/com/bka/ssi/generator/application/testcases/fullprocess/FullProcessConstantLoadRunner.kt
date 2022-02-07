package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process.constant-load-runner.active"],
    matchIfMissing = false
)
class FullProcessConstantLoadRunner(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-cases.full-process.constant-load-runner.number-of-total-iterations}") val numberOfTotalIterations: Int,
    @Value("\${test-cases.full-process.constant-load-runner.number-of-iterations-per-minute}") val numberOfIterationsPerMinute: Int,
    @Value("\${test-cases.full-process.constant-load-runner.core-thread-pool-size}") val coreThreadPoolSize: Int,
    @Value("\${test-cases.full-process.constant-load-runner.use-connectionless-proof-requests}") val useConnectionlessProofRequests: Boolean
) : FullProcessRunner(
    issuerVerifierAriesClient,
    holderAriesClient,
    numberOfTotalIterations,
    useConnectionlessProofRequests
) {

    lateinit var scheduledFuture: ScheduledFuture<*>

    override fun run() {
        logger.info("Starting 'FullProcessTest'...")
        logger.info("Number of Iterations: $numberOfTotalIterations")
        logger.info("Number of Iterations per Minute: $numberOfIterationsPerMinute")

        setUp()

        val executor = Executors.newScheduledThreadPool(coreThreadPoolSize)
        scheduledFuture = executor.scheduleAtFixedRate(
            Runnable { startIteration() },
            0,
            60L / numberOfIterationsPerMinute,
            TimeUnit.SECONDS
        )
    }

    override fun finishedIteration() {
        if (terminateRunner()) {
            scheduledFuture.cancel(false)
        }
    }

}
