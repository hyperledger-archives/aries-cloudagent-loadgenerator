package com.bka.ssi.generator.infrastructure

import org.hyperledger.aries.AriesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class AcaPyClient(
    @Value("\${generator.acapy.api-key}") private val acaPyApiKey: String?,
    @Value("\${generator.acapy.url}") private val acaPyUrl: String?
) {
    var logger: Logger = LoggerFactory.getLogger(AcaPyClient::class.java)

    @Bean
    @Primary
    fun ariesClient(): AriesClient? {
        if (acaPyUrl == null) {
            logger.error("Unable to establish connection to AcaPy. AcaPy URL not configured.")
            return null
        }

        return AriesClient
            .builder()
            .url(acaPyUrl)
            .apiKey(acaPyApiKey)
            .build();
    }
}
