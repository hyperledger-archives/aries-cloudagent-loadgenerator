package com.bka.ssi.generator.domain

class ConnectionInvitationDo(
    var type: String,
    var id: String,
    var recipientKeys: List<String>,
    var serviceEndpoint: String,
    var label: String,
) {
}
