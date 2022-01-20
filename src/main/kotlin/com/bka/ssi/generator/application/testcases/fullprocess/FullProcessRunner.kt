package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.application.testcases.TestRunner
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord
import org.hyperledger.aries.api.revocation.RevocationEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process.active"],
    matchIfMissing = false
)
class FullProcessRunner(
    private val acaPy: AriesClient,
    @Value("\${test-cases.full-process.number-of-iterations}") val numberOfIterations: Int,
    @Value("\${test-cases.full-process.number-of-parallel-iterations}") val numberOfParallelIterations: Int
) : TestRunner() {

    var logger: Logger = LoggerFactory.getLogger(FullProcessRunner::class.java)

    override fun run() {
        logger.info("Starting 'FullProcessTest'...")
        logger.info("Number of Iterations: $numberOfIterations")
        logger.info("Number of Parallel Iterations: $numberOfParallelIterations")

        setUp()

        for (i in 0 until numberOfParallelIterations) {
            startIteration()
        }
    }

    private fun setUp() {
    }

    private fun startIteration() {

    }

    override fun handleConnection(connection: ConnectionRecord?) {
        logger.info("New ConnectionRecord!")
    }

    override fun handleCredential(credential: V1CredentialExchange?) {
        logger.info("New CredentialExchangeRecord!")
    }

    override fun handleRevocation(revocation: RevocationEvent?) {
        logger.info("New ConnectionRevocationEvent!")
    }

    override fun handleProof(proof: PresentationExchangeRecord) {
        logger.info("New PresentationExchangeRecord!")
    }
}
