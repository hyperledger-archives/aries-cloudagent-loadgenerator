package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.logger.ErrorLogger
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
    name = ["test-flows.full-flow.active"],
    matchIfMissing = false
)
class FullFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") holderAriesClients: List<IAriesClient>,
    @Value("\${test-flows.full-flow.use-revocable-credentials}") private val useRevocableCredentials: Boolean,
    @Value("\${test-flows.full-flow.revocation-registry-size}") private val revocationRegistrySize: Int,
    @Value("\${test-flows.full-flow.check-non-revoked}") private val checkNonRevoked: Boolean,
    @Value("\${test-flows.full-flow.revoke-credentials}") private val revokeCredentials: Boolean,
    @Value("\${test-flows.full-flow.credential-revocation-batch-size}") private val credentialRevocationBatchSize: Int,
    @Value("\${test-flows.full-flow.use-oob-instead-of-connection}") private val useOobInsteadOfConnection: Boolean,
    private val errorLogger: ErrorLogger,
) : TestFlow(
    holderAriesClients
) {

    protected companion object {
        const val SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME = "sessionId"

        var numberOfBatchedCredentialRevocations = 0
        var credentialDefinitionId = ""
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing FullFlow...")
        logger.info("use-revocable-credentials: $useRevocableCredentials")
        logger.info("revocation-registry-size: $revocationRegistrySize")
        logger.info("check-non-revoked: $checkNonRevoked")
        logger.info("revoke-credentials: $revokeCredentials")
        logger.info("credential-revocation-batch-size: $credentialRevocationBatchSize")
        logger.info("use-oob-instead-of-connection: $useOobInsteadOfConnection")

        Companion.testRunner = testRunner

        val credentialDefinition = issuerVerifierAriesClient.createSchemaAndCredentialDefinition(
            SchemaDo(
                listOf(SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME),
                "test-credential",
                "1.${Date().time}"
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
                    SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME to UUID.randomUUID().toString()
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

        issuerVerifierAriesClient.issueCredentialToConnection(
            connectionRecord.connectionId,
            CredentialDo(
                credentialDefinitionId,
                mapOf(
                    SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME to UUID.randomUUID().toString()
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
            sendProofRequestOob(credentialExchangeRecord.sessionId)
        } else {
            sendProofRequestToConnection(
                credentialExchangeRecord.sessionId,
                credentialExchangeRecord.connectionId,
                ProofExchangeCommentDo(
                    true,
                    credentialExchangeRecord.sessionId,
                    credentialExchangeRecord.revocationRegistryId,
                    credentialExchangeRecord.revocationRegistryIndex
                )
            )
        }

        logger.info("Sent proof request")
    }

    private fun sendProofRequestToConnection(sessionId: String, connectionId: String, comment: ProofExchangeCommentDo) {
        issuerVerifierAriesClient.sendProofRequestToConnection(
            connectionId,
            ProofRequestDo(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf(SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME),
                        credentialDefinitionId,
                        AttributeValueRestrictionDo(
                            SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME,
                            sessionId
                        )
                    )
                )
            ),
            true,
            comment
        )
    }

    private fun sendProofRequestOob(sessionId: String) {
        val oobProofRequest = issuerVerifierAriesClient.createOobProofRequest(
            ProofRequestDo(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                listOf(
                    CredentialRequestDo(
                        listOf(SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME),
                        credentialDefinitionId,
                        AttributeValueRestrictionDo(
                            SESSION_ID_CREDENTIAL_ATTRIBUTE_NAME,
                            sessionId
                        )
                    )
                )
            ),
            checkNonRevoked
        )

        nextHolderClient().receiveOobProofRequest(oobProofRequest)
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

            if (!revokeCredentials) {
                testRunner?.finishedIteration()
                return
            }

            revokeCredential(
                proofExchangeRecord.connectionId,
                proofExchangeRecord.comment.sessionId,
                proofExchangeRecord.comment.revocationRegistryId,
                proofExchangeRecord.comment.revocationRegistryIndex
            )
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

    private fun revokeCredential(
        connectionId: String,
        sessionId: String,
        revocationRegistryId: String?,
        revocationRegistryIndex: String?
    ) {
        if (revocationRegistryId == null || revocationRegistryIndex == null) {
            errorLogger.reportTestFlowError("Tried to revoke a credential but revocationRegistryId and/or revocationRegistryIndex is missing.")
            return
        }

        val publishRevocations = ++numberOfBatchedCredentialRevocations >= credentialRevocationBatchSize
        if (publishRevocations) {
            revokeCredentialAndPublishRevocationsAndTestCredentialIsRevoked(
                connectionId,
                sessionId,
                revocationRegistryId,
                revocationRegistryIndex
            )
            return
        }

        issuerVerifierAriesClient.revokeCredentialWithoutPublishing(
            CredentialRevocationRegistryRecordDo(
                revocationRegistryId,
                revocationRegistryIndex
            )
        )

        testRunner?.finishedIteration()
    }

    private fun revokeCredentialAndPublishRevocationsAndTestCredentialIsRevoked(
        connectionId: String,
        sessionId: String,
        revocationRegistryId: String,
        revocationRegistryIndex: String
    ) {
        // Reset the revocation counter to ensure that other threads do not attempt to publish revocations too,
        // while the current revocation publishing is still being processed. This may result in many parallel
        // revocation publication processes that will block each other.
        numberOfBatchedCredentialRevocations = 0

        issuerVerifierAriesClient.revokeCredentialAndPublishRevocations(
            CredentialRevocationRegistryRecordDo(
                revocationRegistryId,
                revocationRegistryIndex
            )
        )

        sendProofRequestToConnection(
            sessionId,
            connectionId,
            ProofExchangeCommentDo(
                false,
                sessionId,
                revocationRegistryId,
                revocationRegistryIndex
            )
        )
    }
}
