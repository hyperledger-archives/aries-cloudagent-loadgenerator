package com.bka.ssi.generator.application.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AriesClientLogger() {
    var logger: Logger = LoggerFactory.getLogger(AriesClientLogger::class.java)

    fun publishRevokedCredentials() {
        logger.info(
            "type=publish_credential_revocations time=${
                Instant.now().toEpochMilli()
            }"
        )
    }
}
