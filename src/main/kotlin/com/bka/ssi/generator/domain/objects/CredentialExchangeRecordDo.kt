package com.bka.ssi.generator.domain.objects

class CredentialExchangeRecordDo(
    val id: String,
    val sessionId: String,
    val connectionId: String,
    val time: Long,
    val state: String,
    val issued: Boolean,
    val revocationRegistryId: String?,
    val revocationRegistryIndex: String?
) {
}
