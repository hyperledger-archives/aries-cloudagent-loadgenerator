package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*


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
                listOf("credentialId"),
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
                    "credentialId" to UUID.randomUUID().toString()
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
            ProofExchangeComment(
                true,
                credentialExchangeRecord.internalCredentialId,
                credentialExchangeRecord.revocationRegistryId,
                credentialExchangeRecord.revocationRegistryIndex
            )
        )

        logger.info("Sent proof request")
    }

    private fun sendProofRequestToConnection(connectionId: String, comment: ProofExchangeComment) {
        issuerVerifierAriesClient.sendProofRequestToConnection(
            connectionId,
            ProofRequestDo(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf("credentialId"),
                        credentialDefinitionId
                    )
                )
            ),
            true,
            comment
        )
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.isVerified) {
            return
        }

        if (proofExchangeRecord.comment.shouldBeValid) {
            if (!proofExchangeRecord.isValid) {
                logger.error("Received invalid proof presentation but expected a valid proof presentation")
                return
            }

            logger.info("Received valid proof presentation")

            if (proofExchangeRecord.comment.revocationRegistryId != null && proofExchangeRecord.comment.revocationRegistryIndex != null) {
                issuerVerifierAriesClient.revokeCredential(
                    CredentialRevocationRegistryRecordDo(
                        proofExchangeRecord.comment.revocationRegistryId,
                        proofExchangeRecord.comment.revocationRegistryIndex
                    )
                )

                sendProofRequestToConnection(
                    proofExchangeRecord.connectionId,
                    ProofExchangeComment(
                        false,
                        proofExchangeRecord.comment.credentialId,
                        proofExchangeRecord.comment.revocationRegistryId,
                        proofExchangeRecord.comment.revocationRegistryIndex
                    )
                )
            }
        }

        if (!proofExchangeRecord.comment.shouldBeValid) {
            if (proofExchangeRecord.isValid) {
                logger.error("Credential has not been revoked")
                return
            }

            logger.info("Credential has been successfully revoked")
            testRunner?.finishedIteration()
        }

    }
}
