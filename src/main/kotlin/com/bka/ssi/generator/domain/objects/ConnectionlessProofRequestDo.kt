package com.bka.ssi.generator.domain.objects

class ConnectionlessProofRequestDo(
    val nonRevokedFrom: Long,
    val nonRevokedTo: Long,
    val requestedCredentials: List<CredentialRequestDo>
) {
}
