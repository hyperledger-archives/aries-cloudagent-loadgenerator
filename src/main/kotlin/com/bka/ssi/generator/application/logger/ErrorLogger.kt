package com.bka.ssi.generator.application.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ErrorLogger() {
    var logger: Logger = LoggerFactory.getLogger(ErrorLogger::class.java)

    fun reportTestFlowError(message: String) {
        logger.error(
            "type=test_flow_error message=$message time=${
                Instant.now().toEpochMilli()
            }"
        )
    }

    fun reportTestRunnerError(message: String) {
        logger.error(
            "type=test_runner_error message=$message time=${
                Instant.now().toEpochMilli()
            }"
        )
    }

    fun reportAriesEventError(message: String) {
        logger.error(
            "type=aries_event_error message=$message time=${
                Instant.now().toEpochMilli()
            }"
        )
    }

    fun reportAriesClientError(message: String) {
        logger.error(
            "type=aries_client_error message=$message time=${
                Instant.now().toEpochMilli()
            }"
        )
    }
}
