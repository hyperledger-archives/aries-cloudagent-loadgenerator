package com.bka.ssi.generator.domain

class ConnectionRecordDo(
    val connectionId: String,
    val time: Long,
    val state: String,
    val active: Boolean
) {
}
