package com.bka.ssi.generator.application.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ErrorLogger() {
    var logger: Logger = LoggerFactory.getLogger(ErrorLogger::class.java)

    fun reportError(message: String) {
        logger.error(
            "type=error message=$message time=${
                Instant.now().toEpochMilli()
            }"
        )
    }
}
