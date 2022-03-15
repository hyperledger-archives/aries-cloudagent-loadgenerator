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
    name = ["test-flows.revocation-flow.active"],
    matchIfMissing = false
)
class RevocationFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") holderAriesClients: List<IAriesClient>,
    @Value("\${test-flows.revocation-flow.revocation-registry-size}") private val revocationRegistrySize: Int,
) : TestFlow(
    holderAriesClients
) {

    protected companion object {
        var credentialDefinitionId = ""
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing RevocationFlow...")
        logger.info("revocation-registry-size: $revocationRegistrySize")

        Companion.testRunner = testRunner

        val credentialDefinition = issuerVerifierAriesClient.createSchemaAndCredentialDefinition(
            SchemaDo(
                listOf("first name", "last name"),
                "name",
                "1.0"
            ),
            true,
            revocationRegistrySize
        )
        credentialDefinitionId = credentialDefinition.id

        testRunner.finishedInitialization()
    }

    override fun startIteration() {
        initiateConnection()
    }

    private fun initiateConnection() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        nextHolderClient().receiveConnectionInvitation(connectionInvitation)
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

        sendProofRequestToConnection(
            credentialExchangeRecord.connectionId,
            "Should succeed as the credential has NOT been revoked yet"
        )

        // NOT a good idea as it will cause a race condition between holder and issuer!

        if (credentialExchangeRecord.revocationRegistryId != null && credentialExchangeRecord.revocationIndex != null) {
            issuerVerifierAriesClient.revokeCredential(
                CredentialRevocationRegistryRecordDo(
                    credentialExchangeRecord.revocationRegistryId,
                    credentialExchangeRecord.revocationIndex
                )
            )

            sendProofRequestToConnection(
                credentialExchangeRecord.connectionId,
                "Should fail as the credential has been revoked"
            )
        }

        logger.info("Sent proof request")
    }

    private fun sendProofRequestToConnection(connectionId: String, comment: String) {
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
            true,
            comment
        )
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.verifiedAndValid) {
            return
        }

        if (proofExchangeRecord.state == "Should succeed") {

        }

        if (proofExchangeRecord.state == "Should fail") {

        }


        logger.info("Received valid proof presentation")

        testRunner?.finishedIteration()
    }
}
