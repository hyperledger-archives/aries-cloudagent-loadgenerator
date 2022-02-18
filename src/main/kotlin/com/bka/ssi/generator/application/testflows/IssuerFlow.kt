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
    @Qualifier("Holder") holderAriesClients: List<IAriesClient>,
    @Value("\${test-flows.issuer-flow.use-revocable-credentials}") private val useRevocableCredentials: Boolean,
    @Value("\${test-flows.issuer-flow.revocation-registry-size}") private val revocationRegistrySize: Int,
    @Value("\${test-flows.issuer-flow.use-oob-credential-issuance}") private val useOobCredentialIssuance: Boolean,
) : TestFlow(holderAriesClients) {

    protected companion object {
        var credentialDefinitionId = ""
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing IssuerFlow...")
        logger.info("use-revocable-credentials: $useRevocableCredentials")
        logger.info("revocation-registry-size: $revocationRegistrySize")
        logger.info("use-oob-credential-issuance: $useOobCredentialIssuance")

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
        if (useOobCredentialIssuance) {
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

        nextHolderClient().receiveOobCredentialOffer(oobCredentialOffer)
    }

    private fun initiateConnection() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        nextHolderClient().receiveConnectionInvitation(connectionInvitation)
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
        throw NotImplementedError("The issuer flow does not handle proof exchange records.")
    }
}
