package com.bka.ssi.generator.domain.services

import com.bka.ssi.generator.domain.objects.*

interface IAriesClient {
    fun getPublicDid(): String?
    fun createSchemaAndCredentialDefinition(schemaDo: SchemaDo): CredentialDefinitionDo
    fun createConnectionInvitation(alias: String): ConnectionInvitationDo
    fun receiveConnectionInvitation(connectionInvitationDo: ConnectionInvitationDo)
    fun issueCredential(credentialDo: CredentialDo)
    fun sendProofRequest(proofRequestDo: ProofRequestDo)
}
