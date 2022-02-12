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
    name = ["test-flows.full-flow.active"],
    matchIfMissing = false
)
class FullFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-flows.full-flow.use-revocable-credentials}") private val useRevocableCredentials: Boolean,
    @Value("\${test-flows.full-flow.revocation-registry-size}") private val revocationRegistrySize: Int,
    @Value("\${test-flows.full-flow.check-non-revoked}") private val checkNonRevoked: Boolean,
    @Value("\${test-flows.full-flow.use-oob-instead-of-connection}") private val useOobInsteadOfConnection: Boolean,
) : TestFlow() {

    protected companion object {
        var credentialDefinitionId = ""
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
            useRevocableCredentials,
            revocationRegistrySize
        )
        credentialDefinitionId = credentialDefinition.id

        testRunner.finishedInitialization()
    }

    override fun startIteration() {
        if (useOobInsteadOfConnection) {
            issueCredentialOob()
            return
        }

        initiateConnection()
    }

    private fun issueCredentialOob() {
        val oobCredentialOffer = issuerVerifierAriesClient.createOobCredentialOffer(
            CredentialDo(
                credentialDefinitionId,
                mapOf(
                    "first name" to "Holder",
                    "last name" to "Mustermann"
                )
            )
        )

        holderAriesClient.receiveOobCredentialOffer(oobCredentialOffer)
    }

    private fun initiateConnection() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        holderAriesClient.receiveConnectionInvitation(connectionInvitation)
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        if (!connectionRecord.active) {
            return
        }

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

        logger.info("Issued credential to new connection")
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        if (!credentialExchangeRecord.issued) {
            return
        }

        if (useOobInsteadOfConnection) {
            sendProofRequestOob()
        } else {
            sendProofRequestToConnection(credentialExchangeRecord.connectionId)
        }

        logger.info("Sent proof request")
    }

    private fun sendProofRequestToConnection(connectionId: String) {
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

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.verifiedAndValid) {
            return
        }

        logger.info("Received valid proof presentation")

        testRunner?.finishedIteration()
    }
}
