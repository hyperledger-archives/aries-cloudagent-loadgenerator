package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant


@Service
@ConditionalOnProperty(
    name = ["test-flows.proof-request-flow.active"],
    matchIfMissing = false
)
class ProofRequestFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-flows.proof-request-flow.revocation-registry-size}") private val revocationRegistrySize: Int,
    @Value("\${test-flows.proof-request-flow.check-non-revoked}") private val checkNonRevoked: Boolean,
    @Value("\${test-flows.proof-request-flow.use-oob-proof-requests}") private val useOobProofRequests: Boolean,
) : TestFlow() {

    protected companion object {
        var credentialDefinitionId = ""
        var connectionId = ""
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing test flow...")

        Companion.testRunner = testRunner

        val credentialDefinition = issuerVerifierAriesClient.createSchemaAndCredentialDefinition(
            SchemaDo(
                listOf("first name", "last name"),
                "name",
                "1.0"
            ),
            checkNonRevoked,
            revocationRegistrySize
        )
        credentialDefinitionId = credentialDefinition.id

        initiateConnection()
    }

    override fun startIteration() {
        if (useOobProofRequests) {
            sendProofRequestOob()
        } else {
            sendProofRequestToConnection()
        }

        logger.info("Sent proof request")
    }

    private fun sendProofRequestToConnection() {
        issuerVerifierAriesClient.sendProofRequestToConnection(
            connectionId,
            ProofRequestDo(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf("first name", "last name"),
                        credentialDefinitionId
                    )
                )
            ),
            checkNonRevoked
        )
    }

    private fun sendProofRequestOob() {
        val oobProofRequest = issuerVerifierAriesClient.createOobProofRequest(
            ProofRequestDo(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf("first name", "last name"),
                        credentialDefinitionId
                    )
                )
            ),
            checkNonRevoked
        )

        holderAriesClient.receiveOobProofRequest(oobProofRequest)
    }

    private fun initiateConnection() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        try {
            holderAriesClient.receiveConnectionInvitation(connectionInvitation)
        } catch (exception: Exception) {
            logger.error("${exception.message} (Connection Invitation: ${connectionInvitation.toString()})")
            testRunner?.finishedIteration()
            return
        }
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        if (!connectionRecord.active) {
            return
        }

        logger.info("Established new connection")
        connectionId = connectionRecord.connectionId

        issuerVerifierAriesClient.issueCredentialToConnection(
            connectionRecord.connectionId,
            CredentialDo(
                credentialDefinitionId,
                mapOf(
                    "first name" to "Holder",
                    "last name" to "Mustermann"
                )
            )
        )
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        if (!credentialExchangeRecord.issued) {
            return
        }

        testRunner?.finishedInitialization()
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.verifiedAndValid) {
            return
        }

        logger.info("Received valid proof presentation")

        testRunner?.finishedIteration()
    }
}
