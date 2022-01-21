package com.bka.ssi.generator.infrastructure.ariesclient

import com.bka.ssi.generator.domain.*

interface IAriesClient {
    fun getPublicDid(): String?
    fun createSchemaAndCredentialDefinition(schemaDo: SchemaDo): CredentialDefinitionDo
    fun createConnectionInvitation(alias: String): ConnectionInvitationDo
    fun receiveConnectionInvitation(connectionInvitationDo: ConnectionInvitationDo)
    fun issueCredential(credentialDo: CredentialDo)
    fun sendProofRequest(proofRequestDo: ProofRequestDo)
}
