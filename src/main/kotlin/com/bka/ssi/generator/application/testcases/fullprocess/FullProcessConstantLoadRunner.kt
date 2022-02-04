package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*
import kotlin.concurrent.schedule


@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process-constant-load.active"],
    matchIfMissing = false
)
class FullProcessConstantLoadRunner(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-cases.full-process-constant-load.number-of-iterations}") val numberOfIterations: Int,
    @Value("\${test-cases.full-process-constant-load.number-of-iterations-per-minute}") val numberOfIterationsPerMinute: Int
) : FullProcessRunner(
    issuerVerifierAriesClient,
    holderAriesClient,
    numberOfIterations
) {

    override fun run() {
        logger.info("Starting 'FullProcessTest'...")
        logger.info("Number of Iterations: $numberOfIterations")
        logger.info("Number of Iterations per Minute: $numberOfIterationsPerMinute")

        setUp()

        Timer("Start Iteration", true).schedule(0L, 60000L / numberOfIterationsPerMinute) {
            startIteration()
        }
    }

}
