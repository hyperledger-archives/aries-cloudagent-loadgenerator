package com.bka.ssi.generator.application.testcases.fullprocess

import com.bka.ssi.generator.application.testcases.TestRunner
import com.google.gson.JsonObject
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.connection.CreateInvitationParams
import org.hyperledger.aries.api.connection.CreateInvitationRequest
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest
import org.hyperledger.aries.api.credential_definition.CredentialDefinition
import org.hyperledger.aries.api.credentials.CredentialAttributes
import org.hyperledger.aries.api.credentials.CredentialPreview
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest
import org.hyperledger.aries.api.present_proof.PresentProofRequest
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord
import org.hyperledger.aries.api.revocation.RevocationEvent
import org.hyperledger.aries.api.schema.SchemaSendRequest
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
    @Qualifier("IssuerVerifier") private val issuerVerifierAcaPy: AriesClient,
    @Qualifier("Holder") private val holderAcaPy: AriesClient,
    @Value("\${test-cases.full-process.number-of-iterations}") val numberOfIterations: Int,
    @Value("\${test-cases.full-process.number-of-parallel-iterations}") val numberOfParallelIterations: Int
) : TestRunner() {

    companion object {
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
        val schemaSendResponse = issuerVerifierAcaPy.schemas(
            SchemaSendRequest.builder()
                .attributes(listOf("first name", "last name"))
                .schemaName("name")
                .schemaVersion("1.0")
                .build()
        )

        if (schemaSendResponse.isEmpty) {
            throw Exception("Failed to create schema.")
        }

        val credentialDefinitionResponse = issuerVerifierAcaPy.credentialDefinitionsCreate(
            CredentialDefinition.CredentialDefinitionRequest.builder()
                .schemaId(schemaSendResponse.get().schemaId)
                .revocationRegistrySize(500)
                .supportRevocation(true)
                .tag("1.0")
                .build()
        )

        if (credentialDefinitionResponse.isEmpty) {
            throw Exception("Failed to create credential definition.")
        }

        FullProcessRunner.credentialDefinitionId = credentialDefinitionResponse.get().credentialDefinitionId

        logger.info("Setup completed")
    }

    private fun startIteration() {
        if (FullProcessRunner.numberOfIterationsStarted >= numberOfIterations) {
            return
        }

        val createInvitationRequest = issuerVerifierAcaPy.connectionsCreateInvitation(
            CreateInvitationRequest.builder().build(),
            CreateInvitationParams(
                "holder-acapy",
                true,
                false,
                false
            )
        )

        if (createInvitationRequest.isEmpty) {
            throw Exception("Failed to create connection invitation.")
        }

        holderAcaPy.connectionsReceiveInvitation(
            ReceiveInvitationRequest.builder()
                .type(createInvitationRequest.get().invitation.atType)
                .id(createInvitationRequest.get().invitation.atId)
                .recipientKeys(createInvitationRequest.get().invitation.recipientKeys)
                .serviceEndpoint(createInvitationRequest.get().invitation.serviceEndpoint)
                .label(createInvitationRequest.get().invitation.label)
                .build(),
            null
        )

        FullProcessRunner.numberOfIterationsStarted++

        logger.info("Started ${FullProcessRunner.numberOfIterationsStarted} of $numberOfIterations iteration")
    }

    override fun handleConnection(connection: ConnectionRecord) {
        if (!connection.stateIsActive()) {
            return
        }

        issuerVerifierAcaPy.issueCredentialSend(
            V1CredentialProposalRequest(
                true,
                true,
                "name credential offer",
                connection.connectionId,
                FullProcessRunner.credentialDefinitionId,
                CredentialPreview(
                    listOf(
                        CredentialAttributes("first name", "Holder"),
                        CredentialAttributes("last name", "Mustermann")
                    )
                ),
                null,
                null,
                null,
                null,
                null,
                false
            )
        )

        logger.info("Issued credential to new connection")
    }

    override fun handleCredential(credential: V1CredentialExchange) {
        if (!credential.stateIsCredentialAcked()) {
            return
        }

        val nameCredentialRestriction = JsonObject()
        nameCredentialRestriction.addProperty("cred_def_id", credentialDefinitionId)

        issuerVerifierAcaPy.presentProofSendRequest(
            PresentProofRequest(
                credential.connectionId,
                PresentProofRequest.ProofRequest.builder()
                    .name("Proof Request")
                    .nonRevoked(
                        PresentProofRequest.ProofRequest.ProofNonRevoked(
                            Instant.now().toEpochMilli(),
                            Instant.now().toEpochMilli()
                        )
                    )
                    .requestedAttributes(
                        mapOf(
                            "nameCredential" to PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                .names(listOf("first name", "last name"))
                                .restriction(
                                    nameCredentialRestriction
                                )
                                .build()
                        )
                    )
                    .version("1.0")
                    .build(),
                false,
                "name credential proof request"
            )
        )
        logger.info("Send proof request")
    }

    override fun handleRevocation(revocation: RevocationEvent) {
    }

    override fun handleProof(proof: PresentationExchangeRecord) {
        if (!proof.isVerified) {
            return
        }

        logger.info("Received proof presentation")

        startIteration()
    }
}
