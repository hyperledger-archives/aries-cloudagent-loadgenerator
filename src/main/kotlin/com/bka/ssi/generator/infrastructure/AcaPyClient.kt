package com.bka.ssi.generator.infrastructure

import org.hyperledger.aries.AriesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AcaPyClient(
    @Value("\${issuer-verifier.acapy.api-key}") private val issuerVerifierAcaPyApiKey: String?,
    @Value("\${issuer-verifier.acapy.url}") private val issuerVerifierAcaPyUrl: String?,
    @Value("\${holder.acapy.api-key}") private val holderAcaPyApiKey: String?,
    @Value("\${holder.acapy.url}") private val holderAcaPyUrl: String?
) {
    var logger: Logger = LoggerFactory.getLogger(AcaPyClient::class.java)

    @Bean(name = ["IssuerVerifier"])
    fun issuerVerifierAriesClient(): AriesClient? {
        if (issuerVerifierAcaPyUrl == null) {
            logger.error("Unable to establish connection to Issuer/Verifier AcaPy. Issuer/Verifier AcaPy URL not configured.")
            return null
        }
        return AriesClient.builder().url(issuerVerifierAcaPyUrl).apiKey(issuerVerifierAcaPyApiKey).build()
    }

    @Bean(name = ["Holder"])
    fun holderAriesClient(): AriesClient? {
        if (holderAcaPyUrl == null) {
            logger.error("Unable to establish connection to Holder AcaPy. Holder AcaPy URL not configured.")
            return null
        }
        return AriesClient.builder().url(holderAcaPyUrl).apiKey(holderAcaPyApiKey).build()
    }
}
