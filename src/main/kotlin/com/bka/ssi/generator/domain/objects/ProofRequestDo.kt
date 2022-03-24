package com.bka.ssi.generator.domain.objects

class ProofRequestDo(
    val nonRevokedFrom: Long,
    val nonRevokedTo: Long,
    val requestedCredentials: List<CredentialRequestDo>
) {
}

class CredentialRequestDo(
    val claims: List<String>,
    val credentialDefinitionIdRestriction: String,
    val attributeValueRestriction: AttributeValueRestrictionDo
)

class AttributeValueRestrictionDo(
    val attributeName: String,
    val attributeValue: String
)
