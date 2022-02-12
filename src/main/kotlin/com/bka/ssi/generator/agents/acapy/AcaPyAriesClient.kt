package com.bka.ssi.generator.agents.acapy

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
import org.hyperledger.aries.api.schema.SchemaSendRequest


class AcaPyAriesClient(
    private val acaPy: AriesClient,
    private val errorLogger: ErrorLogger
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
            errorLogger.reportError("AcaPyAriesClient.createSchemaAndCredentialDefinition: Failed to create schema.")
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
            errorLogger.reportError("AcaPyAriesClient.createSchemaAndCredentialDefinition: Failed to create credential definition.")
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
            errorLogger.reportError("AcaPyAriesClient.createConnectionInvitation: Failed to create connection invitation.")
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
            errorLogger.reportError("AcaPyAriesClient.receiveConnectionInvitation: Failed to receive connection invitation.")
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
            errorLogger.reportError("AcaPyAriesClient.issueCredentialToConnection: Failed to issue credential.")
        }
    }

    override fun createOobCredentialOffer(credentialDo: CredentialDo): OobCredentialOfferDo {
        errorLogger.reportError("AcaPyAriesClient.createOobCredentialOffer: Creating an OOB Credential Offer is not implemented yet.")
        throw NotImplementedError("Creating an OOB Credential Offer is not implemented yet.")
    }

    override fun receiveOobCredentialOffer(oobCredentialOfferDo: OobCredentialOfferDo) {
        errorLogger.reportError("AcaPyAriesClient.receiveOobCredentialOffer: Receiving an OOB Credential Offer is not implemented yet.")
        throw NotImplementedError("Receiving an OOB Credential Offer is not implemented yet.")
    }

    override fun sendProofRequestToConnection(
        connectionId: String,
        proofRequestDo: ProofRequestDo,
        checkNonRevoked: Boolean
    ) {
        val proofRequestBuilder = PresentProofRequest.ProofRequest.builder()
            .name("Proof Request")
            .requestedAttributes(
                proofRequestDo.requestedCredentials.mapIndexed { index: Int, credentialRequestDo: CredentialRequestDo ->
                    "${index}_credential" to PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                        .names(credentialRequestDo.claims)
                        .restriction(
                            Gson().fromJson(
                                "{\"cred_def_id\": \"${credentialRequestDo.credentialDefinitionId}\"}",
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
            errorLogger.reportError("AcaPyAriesClient.sendProofRequestToConnection: Failed to create and send proof request to connectionId.")
        }
    }

    override fun createOobProofRequest(proofRequestDo: ProofRequestDo, checkNonRevoked: Boolean): OobProofRequestDo {
        errorLogger.reportError("AcaPyAriesClient.createOobProofRequest: Creating an OOB Proof Request is not implemented yet.")
        throw NotImplementedError("Creating an OOB Proof Request is not implemented yet.")
    }

    override fun receiveOobProofRequest(oobProofRequestDo: OobProofRequestDo) {
        errorLogger.reportError("AcaPyAriesClient.receiveOobProofRequest: Receiving an OOB Proof Request is not implemented yet.")
        throw NotImplementedError("Receiving an OOB Proof Request is not implemented yet.")
    }
}
