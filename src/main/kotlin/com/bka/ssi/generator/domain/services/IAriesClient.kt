package com.bka.ssi.generator.domain.services

import com.bka.ssi.generator.domain.objects.*

interface IAriesClient {
    fun getPublicDid(): String?

    fun createSchemaAndCredentialDefinition(
        schemaDo: SchemaDo, revocable: Boolean,
        revocationRegistrySize: Int
    ): CredentialDefinitionDo

    fun createConnectionInvitation(alias: String): ConnectionInvitationDo
    fun receiveConnectionInvitation(connectionInvitationDo: ConnectionInvitationDo)
    fun issueCredentialToConnection(connectionId: String, credentialDo: CredentialDo)
    fun sendProofRequestToConnection(connectionId: String, proofRequestDo: ProofRequestDo, checkNonRevoked: Boolean)
    fun createOobProofRequest(proofRequestDo: ProofRequestDo, checkNonRevoked: Boolean): OobProofRequestDo
    fun receiveOobProofRequest(oobProofRequestDo: OobProofRequestDo)
}
