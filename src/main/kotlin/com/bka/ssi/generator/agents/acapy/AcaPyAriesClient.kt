package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.application.logger.AriesClientLogger
import com.bka.ssi.generator.application.logger.ErrorLogger
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.CreateInvitationParams
import org.hyperledger.aries.api.connection.CreateInvitationRequest
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest
import org.hyperledger.aries.api.credential_definition.CredentialDefinition
import org.hyperledger.aries.api.credentials.CredentialAttributes
import org.hyperledger.aries.api.credentials.CredentialPreview
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest
import org.hyperledger.aries.api.present_proof.PresentProofRequest
import org.hyperledger.aries.api.revocation.RevokeRequest
import org.hyperledger.aries.api.schema.SchemaSendRequest
import java.util.*


class AcaPyAriesClient(
    private val acaPy: AriesClient,
    private val errorLogger: ErrorLogger,
    private val ariesClientLogger: AriesClientLogger
) : IAriesClient {

    override fun getPublicDid(): String? {
        val did = acaPy.walletDidPublic().orElse(null) ?: return null

        return did.did
    }

    override fun createSchemaAndCredentialDefinition(
        schemaDo: SchemaDo,
        revocable: Boolean,
        revocationRegistrySize: Int
    ): CredentialDefinitionDo {
        val schemaSendResponse = acaPy.schemas(
            SchemaSendRequest.builder()
                .attributes(schemaDo.attributes)
                .schemaName(schemaDo.name)
                .schemaVersion(schemaDo.version)
                .build()
        )

        if (schemaSendResponse.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.createSchemaAndCredentialDefinition: Failed to create schema.")
        }

        val credentialDefinitionResponse = acaPy.credentialDefinitionsCreate(
            CredentialDefinition.CredentialDefinitionRequest.builder()
                .schemaId(schemaSendResponse.get().schemaId)
                .supportRevocation(revocable)
                .revocationRegistrySize(revocationRegistrySize)
                .tag("1.0")
                .build()
        )

        if (credentialDefinitionResponse.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.createSchemaAndCredentialDefinition: Failed to create credential definition.")
        }

        return CredentialDefinitionDo(credentialDefinitionResponse.get().credentialDefinitionId)
    }

    override fun createConnectionInvitation(alias: String): ConnectionInvitationDo {
        val createInvitationRequest = acaPy.connectionsCreateInvitation(
            CreateInvitationRequest.builder().build(),
            CreateInvitationParams(
                alias,
                true,
                false,
                false
            )
        )

        if (createInvitationRequest.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.createConnectionInvitation: Failed to create connection invitation.")
        }

        return ConnectionInvitationDo(
            createInvitationRequest.get().invitation.atType,
            createInvitationRequest.get().invitation.atId,
            createInvitationRequest.get().invitation.recipientKeys,
            createInvitationRequest.get().invitation.serviceEndpoint,
            createInvitationRequest.get().invitation.label
        )
    }

    override fun receiveConnectionInvitation(connectionInvitationDo: ConnectionInvitationDo) {
        val connectionRecord = acaPy.connectionsReceiveInvitation(
            ReceiveInvitationRequest.builder()
                .type(connectionInvitationDo.type)
                .id(connectionInvitationDo.id)
                .recipientKeys(connectionInvitationDo.recipientKeys)
                .serviceEndpoint(connectionInvitationDo.serviceEndpoint)
                .label(connectionInvitationDo.label)
                .build(),
            null
        )

        if (connectionRecord.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.receiveConnectionInvitation: Failed to receive connection invitation.")
        }
    }

    override fun issueCredentialToConnection(connectionId: String, credentialDo: CredentialDo) {
        val credentialExchange = acaPy.issueCredentialSend(
            V1CredentialProposalRequest(
                true,
                true,
                "Credential Offer",
                connectionId,
                credentialDo.credentialDefinitionId,
                CredentialPreview(
                    credentialDo.claims.map { CredentialAttributes(it.key, it.value) }
                ),
                null,
                null,
                null,
                null,
                null,
                false
            )
        )

        if (credentialExchange.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.issueCredentialToConnection: Failed to issue credential.")
        }
    }

    private fun revokeCredential(
        credentialRevocationRegistryRecord: CredentialRevocationRegistryRecordDo,
        publish: Boolean
    ) {
        val credentialRevocation = acaPy.revocationRevoke(
            RevokeRequest.builder()
                .credRevId(credentialRevocationRegistryRecord.credentialRevocationRegistryIndex)
                .revRegId(credentialRevocationRegistryRecord.credentialRevocationRegistryId)
                .publish(publish)
                .notify(false)
                .build()
        )

        if (credentialRevocation.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.revokeCredential: Failed to revoke credential.")
        }
    }

    override fun revokeCredentialWithoutPublishing(credentialRevocationRegistryRecord: CredentialRevocationRegistryRecordDo) {
        revokeCredential(credentialRevocationRegistryRecord, false)
    }

    override fun revokeCredentialAndPublishRevocations(credentialRevocationRegistryRecord: CredentialRevocationRegistryRecordDo) {
        val trackingId = UUID.randomUUID().toString()

        ariesClientLogger.startPublishRevokedCredentials(trackingId)
        revokeCredential(credentialRevocationRegistryRecord, true)
        ariesClientLogger.stopPublishRevokedCredentials(trackingId)
    }

    override fun createOobCredentialOffer(credentialDo: CredentialDo): OobCredentialOfferDo {
        errorLogger.reportAriesClientError("AcaPyAriesClient.createOobCredentialOffer: Creating an OOB Credential Offer is not implemented yet.")
        throw NotImplementedError("Creating an OOB Credential Offer is not implemented yet.")
    }

    override fun receiveOobCredentialOffer(oobCredentialOfferDo: OobCredentialOfferDo) {
        errorLogger.reportAriesClientError("AcaPyAriesClient.receiveOobCredentialOffer: Receiving an OOB Credential Offer is not implemented yet.")
        throw NotImplementedError("Receiving an OOB Credential Offer is not implemented yet.")
    }

    override fun sendProofRequestToConnection(
        connectionId: String,
        proofRequestDo: ProofRequestDo,
        checkNonRevoked: Boolean,
        comment: ProofExchangeCommentDo
    ) {
        val proofRequestBuilder = PresentProofRequest.ProofRequest.builder()
            .name(comment.toString())
            .requestedAttributes(
                proofRequestDo.requestedCredentials.mapIndexed { index: Int, credentialRequestDo: CredentialRequestDo ->
                    "${index}_credential" to PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                        .names(credentialRequestDo.claims)
                        .restriction(
                            Gson().fromJson(
                                "{\"cred_def_id\": \"${credentialRequestDo.credentialDefinitionIdRestriction}\"}",
                                JsonObject::class.java
                            )
                        )
                        .restriction(
                            Gson().fromJson(
                                "{\"attr::${credentialRequestDo.attributeValueRestriction.attributeName}::value\": \"${credentialRequestDo.attributeValueRestriction.attributeValue}\"}",
                                JsonObject::class.java
                            )
                        )
                        .build()
                }.toMap()
            )
            .version("1.0")

        if (checkNonRevoked) {
            proofRequestBuilder.nonRevoked(
                PresentProofRequest.ProofRequest.ProofNonRevoked(
                    proofRequestDo.nonRevokedFrom,
                    proofRequestDo.nonRevokedTo
                )
            )
        }

        val proofRequest = proofRequestBuilder.build()

        val presentationExchangeRecord = acaPy.presentProofSendRequest(
            PresentProofRequest(
                connectionId,
                proofRequest,
                false,
                "Proof Request"
            )
        )

        if (presentationExchangeRecord.isEmpty) {
            errorLogger.reportAriesClientError("AcaPyAriesClient.sendProofRequestToConnection: Failed to create and send proof request to connectionId.")
        }
    }

    override fun createOobProofRequest(proofRequestDo: ProofRequestDo, checkNonRevoked: Boolean): OobProofRequestDo {
        errorLogger.reportAriesClientError("AcaPyAriesClient.createOobProofRequest: Creating an OOB Proof Request is not implemented yet.")
        throw NotImplementedError("Creating an OOB Proof Request is not implemented yet.")
    }

    override fun receiveOobProofRequest(oobProofRequestDo: OobProofRequestDo) {
        errorLogger.reportAriesClientError("AcaPyAriesClient.receiveOobProofRequest: Receiving an OOB Proof Request is not implemented yet.")
        throw NotImplementedError("Receiving an OOB Proof Request is not implemented yet.")
    }
}
