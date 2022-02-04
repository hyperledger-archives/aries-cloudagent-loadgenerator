package com.bka.ssi.generator.domain.objects

class CredentialDo(
    val credentialDefinitionId: String,
    val claims: Map<String, String>
) {
}
