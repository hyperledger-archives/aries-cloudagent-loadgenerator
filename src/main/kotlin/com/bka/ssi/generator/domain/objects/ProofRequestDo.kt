package com.bka.ssi.generator.domain.objects

class ProofRequestDo(
    val nonRevokedFrom: Long,
    val nonRevokedTo: Long,
    val requestedCredentials: List<CredentialRequestDo>
) {
}

class CredentialRequestDo(
    val claims: List<String>,
    val credentialDefinitionId: String
)
