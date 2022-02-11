package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service


@Service
@ConditionalOnProperty(
    name = ["test-flows.issuer-flow.active"],
    matchIfMissing = false
)
class IssuerFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-flows.issuer-flow.use-oob-credential-issuance}") private val useOobCredentialIssuance: Boolean,
    @Value("\${test-flows.issuer-flow.use-revocable-credentials}") private val useRevocableCredentials: Boolean,
    @Value("\${test-flows.issuer-flow.revocation-registry-size}") private val revocationRegistrySize: Int
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

        logger.info("Initialization completed.")
    }

    override fun startIteration() {
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

        logger.info("Issued credential")

        testRunner?.finishedIteration()
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
    }
}
