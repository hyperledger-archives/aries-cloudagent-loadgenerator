package com.bka.ssi.generator.domain

class ProofRequestDo(
    val connectionId: String,
    val nonRevokedFrom: Long,
    val nonRevokedTo: Long,
    val requestedCredentials: List<CredentialRequestDo>
) {
}

class CredentialRequestDo(
    val claims: List<String>,
    val credentialDefinitionId: String
)
