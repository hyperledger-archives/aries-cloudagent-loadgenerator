package com.bka.ssi.generator.domain

class CredentialDo(
    val connectionId: String,
    val credentialDefinitionId: String,
    val claims: Map<String, String>
) {
}
