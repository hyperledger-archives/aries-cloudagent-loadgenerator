package com.bka.ssi.generator.domain.objects

class CredentialExchangeRecordDo(
    val id: String,
    val connectionId: String,
    val time: Long,
    val state: String,
    val issued: Boolean,
) {
}
