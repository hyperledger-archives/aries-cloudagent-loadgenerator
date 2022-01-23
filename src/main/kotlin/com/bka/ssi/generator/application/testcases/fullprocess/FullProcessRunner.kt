package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.application.testcases.TestRunner
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@ConditionalOnProperty(
    name = ["test-cases.full-process.active"],
    matchIfMissing = false
)
class FullProcessRunner(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-cases.full-process.number-of-iterations}") val numberOfIterations: Int,
    @Value("\${test-cases.full-process.number-of-parallel-iterations}") val numberOfParallelIterations: Int
) : TestRunner() {

    private companion object {
        var credentialDefinitionId = ""
        var numberOfIterationsStarted = 0
    }

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
        val credentialDefinition = issuerVerifierAriesClient.createSchemaAndCredentialDefinition(
            SchemaDo(
                listOf("first name", "last name"),
                "name",
                "1.0"
            )
        )


        FullProcessRunner.credentialDefinitionId = credentialDefinition.id

        logger.info("Setup completed")
    }

    private fun startIteration() {
        if (terminateRunner()) {
            return
        }

        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        holderAriesClient.receiveConnectionInvitation(connectionInvitation)


        FullProcessRunner.numberOfIterationsStarted++

        logger.info("Started ${FullProcessRunner.numberOfIterationsStarted} of $numberOfIterations iteration")
    }

    private fun terminateRunner(): Boolean {
        return FullProcessRunner.numberOfIterationsStarted >= numberOfIterations
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        if (!connectionRecord.active) {
            return
        }

        issuerVerifierAriesClient.issueCredential(
            CredentialDo(
                connectionRecord.connectionId,
                credentialDefinitionId,
                mapOf(
                    "first name" to "Holder",
                    "last name" to "Mustermann"
                )
            )
        )

        logger.info("Issued credential to new connection")
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        if (!credentialExchangeRecord.issued) {
            return
        }

        issuerVerifierAriesClient.sendProofRequest(
            ProofRequestDo(
                credentialExchangeRecord.connectionId,
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf("first name", "last name"),
                        credentialDefinitionId
                    )
                )
            )
        )

        logger.info("Send proof request")
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.verifiedAndValid) {
            return
        }

        logger.info("Received valid proof presentation")

        startIteration()
    }
}
