package com.bka.ssi.generator.domain.objects

class CredentialDo(
    val connectionId: String,
    val credentialDefinitionId: String,
    val claims: Map<String, String>
) {
}
