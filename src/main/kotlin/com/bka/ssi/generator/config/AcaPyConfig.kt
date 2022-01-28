/*
 *
 *  * Copyright 2022 Bundesrepublik Deutschland
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.bka.ssi.generator.config

import com.bka.ssi.generator.agents.acapy.AcaPyAriesClient
import com.bka.ssi.generator.agents.acapy.OkHttpPublisher
import com.bka.ssi.generator.domain.services.IAriesClient
import okhttp3.OkHttpClient
import org.hyperledger.aries.AriesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class AcaPyConfig(
    @Value("\${issuer-verifier.acapy.api-key}") private val issuerVerifierAcaPyApiKey: String?,
    @Value("\${issuer-verifier.acapy.url}") private val issuerVerifierAcaPyUrl: String?,
    @Value("\${issuer-verifier.acapy.http-timeout}") private val issuerVerifierAcaPyHttpTimeout: Long,
    @Value("\${holder.acapy.api-key}") private val holderAcaPyApiKey: String?,
    @Value("\${holder.acapy.url}") private val holderAcaPyUrl: String?,
    @Value("\${holder.acapy.http-timeout}") private val holderAcapyHttpTimeout: Long
) {
    var logger: Logger = LoggerFactory.getLogger(AcaPyConfig::class.java)

    @Bean(name = ["IssuerVerifier"])
    fun issuerVerifierAriesClient(okHttpPublisher: OkHttpPublisher): IAriesClient? {
        if (issuerVerifierAcaPyUrl == null) {
            logger.error("Unable to establish connection to Issuer/Verifier AcaPy. Issuer/Verifier AcaPy URL not configured.")
            return null
        }

        val issuerVerifierAcaPyClient =
            buildAcaPyAriesClient(
                okHttpPublisher,
                issuerVerifierAcaPyUrl,
                issuerVerifierAcaPyApiKey,
                issuerVerifierAcaPyHttpTimeout
            )

        return AcaPyAriesClient(issuerVerifierAcaPyClient)
    }

    @Bean(name = ["Holder"])
    fun holderAriesClient(okHttpPublisher: OkHttpPublisher): IAriesClient? {
        if (holderAcaPyUrl == null) {
            logger.error("Unable to establish connection to Holder AcaPy. Holder AcaPy URL not configured.")
            return null
        }

        val holderAcaPyClient =
            buildAcaPyAriesClient(okHttpPublisher, holderAcaPyUrl, holderAcaPyApiKey, holderAcapyHttpTimeout)

        return AcaPyAriesClient(holderAcaPyClient)
    }

    private fun buildAcaPyAriesClient(
        okHttpPublisher: OkHttpPublisher,
        acaPyUrl: String,
        acaPyApiKey: String?,
        acaPyHttpTimeout: Long
    ): AriesClient {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(okHttpPublisher)
            .writeTimeout(acaPyHttpTimeout, TimeUnit.SECONDS)
            .readTimeout(acaPyHttpTimeout, TimeUnit.SECONDS)
            .connectTimeout(acaPyHttpTimeout, TimeUnit.SECONDS)
            .callTimeout(acaPyHttpTimeout, TimeUnit.SECONDS)
            .build()

        return AriesClient.builder()
            .url(acaPyUrl)
            .apiKey(acaPyApiKey)
            .client(okHttpClient)
            .build()
    }
}
