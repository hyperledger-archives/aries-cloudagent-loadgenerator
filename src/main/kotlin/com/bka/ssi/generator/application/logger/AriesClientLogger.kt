package com.bka.ssi.generator.application.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AriesClientLogger() {
    var logger: Logger = LoggerFactory.getLogger(AriesClientLogger::class.java)

    fun startPublishRevokedCredentials(trackingId: String) {
        logger.info(
            "type=publish_credential_revocations_started trackingId=${trackingId} time=${
                Instant.now().toEpochMilli()
            }"
        )
    }

    fun stopPublishRevokedCredentials(trackingId: String) {
        logger.info(
            "type=publish_credential_revocations_stopped trackingId=${trackingId} time=${
                Instant.now().toEpochMilli()
            }"
        )
    }
}
