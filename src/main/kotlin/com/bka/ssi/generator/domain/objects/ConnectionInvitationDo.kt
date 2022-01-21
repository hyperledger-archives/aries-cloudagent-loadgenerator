package com.bka.ssi.generator.domain.objects

class ConnectionInvitationDo(
    var type: String,
    var id: String,
    var recipientKeys: List<String>,
    var serviceEndpoint: String,
    var label: String,
) {
}
