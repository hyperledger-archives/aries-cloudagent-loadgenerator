package com.bka.ssi.generator.application.logger

import com.bka.ssi.generator.domain.services.IHttpRequestObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HttpRequestLogger : IHttpRequestObserver {
    var logger: Logger = LoggerFactory.getLogger(HttpRequestLogger::class.java)

    override fun logHttpRequest(
        httpMethod: String,
        urlPath: String,
        httpResponseCode: Int,
        durationInMs: Double
    ) {
        logger.info("type=http_request request=${httpMethod}${urlPath} httpCode=${httpResponseCode} durationInMs=${durationInMs}")
    }
}
