package com.bka.ssi.generator.agents.acapy

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
    private val acaPy: AriesClient
) : IAriesClient {

    override fun getPublicDid(): String? {
        val did = acaPy.walletDidPublic().orElse(null) ?: return null

        return did.did
    }

    override fun createSchemaAndCredentialDefinition(schemaDo: SchemaDo): CredentialDefinitionDo {
        val schemaSendResponse = acaPy.schemas(
            SchemaSendRequest.builder()
                .attributes(schemaDo.attributes)
                .schemaName(schemaDo.name)
                .schemaVersion(schemaDo.version)
                .build()
        )

        if (schemaSendResponse.isEmpty) {
            throw Exception("Failed to create schema.")
        }

        val credentialDefinitionResponse = acaPy.credentialDefinitionsCreate(
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
            throw Exception("Failed to create connection invitation.")
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
            throw Exception("Failed to receive connection invitation.")
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
            throw Exception("Failed to issue credential.")
        }
    }

    override fun sendProofRequestToConnection(connectionId: String, proofRequestDo: ProofRequestDo) {
        val presentationExchangeRecord = acaPy.presentProofSendRequest(
            PresentProofRequest(
                connectionId,
                PresentProofRequest.ProofRequest.builder()
                    .name("Proof Request")
                    .nonRevoked(
                        PresentProofRequest.ProofRequest.ProofNonRevoked(
                            proofRequestDo.nonRevokedFrom,
                            proofRequestDo.nonRevokedTo
                        )
                    )
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
                    .build(),
                false,
                "Proof Request"
            )
        )

        if (presentationExchangeRecord.isEmpty) {
            throw Exception("Failed to create and send proof request to connectionId.")
        }
    }

    override fun createOobProofRequest(proofRequestDo: ProofRequestDo): OobProofRequestDo {
        val presentationExchangeRecord = acaPy.presentProofCreateRequest(
            PresentProofRequest(
                null,
                PresentProofRequest.ProofRequest.builder()
                    .name("Proof Request")
                    .nonRevoked(
                        PresentProofRequest.ProofRequest.ProofNonRevoked(
                            proofRequestDo.nonRevokedFrom,
                            proofRequestDo.nonRevokedTo
                        )
                    )
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
                    .build(),
                false,
                "Proof Request"
            )
        )

        if (presentationExchangeRecord.isEmpty) {
            throw Exception("Failed to create oob proof request.")
        }

        return OobProofRequestDo(
            10,
            10,
            emptyList()
        )
    }

    override fun receiveOobProofRequest(oobProofRequestDo: OobProofRequestDo) {
    }
}
