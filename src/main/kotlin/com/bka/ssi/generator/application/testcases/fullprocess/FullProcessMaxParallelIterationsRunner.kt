package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process.max-parallel-iterations-runner.active"],
    matchIfMissing = false
)
class FullProcessMaxParallelIterationsRunner(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-cases.full-process.max-parallel-iterations-runner.number-of-total-iterations}") val numberOfTotalIterations: Int,
    @Value("\${test-cases.full-process.max-parallel-iterations-runner.number-of-parallel-iterations}") val numberOfParallelIterations: Int,
    @Value("\${test-cases.full-process.max-parallel-iterations-runner.use-connectionless-proof-requests}") val useConnectionlessProofRequests: Boolean
) : FullProcessRunner(
    issuerVerifierAriesClient,
    holderAriesClient,
    numberOfTotalIterations,
    useConnectionlessProofRequests
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
