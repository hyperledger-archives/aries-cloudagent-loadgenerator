package com.bka.ssi.generator.domain.objects

class OobProofRequestDo(
    val nonRevokedFrom: Long,
    val nonRevokedTo: Long,
    val requestedCredentials: List<CredentialRequestDo>
) {
}
