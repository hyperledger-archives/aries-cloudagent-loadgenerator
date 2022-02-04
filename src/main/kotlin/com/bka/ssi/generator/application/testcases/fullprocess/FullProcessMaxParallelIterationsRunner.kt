package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process-max-parallel-iterations.active"],
    matchIfMissing = false
)
class FullProcessMaxParallelIterationsRunner(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-cases.full-process-max-parallel-iterations.number-of-total-iterations}") val numberOfTotalIterations: Int,
    @Value("\${test-cases.full-process-max-parallel-iterations.number-of-parallel-iterations}") val numberOfParallelIterations: Int
) : FullProcessRunner(
    issuerVerifierAriesClient,
    holderAriesClient,
    numberOfTotalIterations
) {

    override fun run() {
        logger.info("Starting 'FullProcessTest'...")
        logger.info("Number of Iterations: $numberOfTotalIterations")
        logger.info("Number of Parallel Iterations: $numberOfParallelIterations")

        setUp()

        for (i in 0 until numberOfParallelIterations) {
            startIteration()
        }
    }

    override fun finishedIteration() {
        if (!terminateRunner()) {
            startIteration()
        }
    }
}
