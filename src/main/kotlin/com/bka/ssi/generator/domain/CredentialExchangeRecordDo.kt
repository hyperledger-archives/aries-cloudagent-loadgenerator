package com.bka.ssi.generator.domain

class CredentialExchangeRecordDo(
    val id: String,
    val connectionId: String,
    val time: Long,
    val state: String,
    val issued: Boolean,
) {
}
