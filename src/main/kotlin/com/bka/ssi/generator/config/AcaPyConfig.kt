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
import com.bka.ssi.generator.agents.acapy.AcaPyOkHttpInterceptor
import com.bka.ssi.generator.application.logger.AriesClientLogger
import com.bka.ssi.generator.application.logger.ErrorLogger
import com.bka.ssi.generator.domain.services.IAriesClient
import okhttp3.OkHttpClient
import org.hyperledger.acy_py.generated.model.DID
import org.hyperledger.acy_py.generated.model.DIDCreate
import org.hyperledger.acy_py.generated.model.DIDCreateOptions
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.multitenancy.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import java.util.*
import java.util.concurrent.TimeUnit

@Configuration
class AcaPyConfig(
    private val errorLogger: ErrorLogger,
    private val ariesClientLogger: AriesClientLogger,
    @Value("\${issuer-verifier.acapy.api-key}") private val issuerVerifierAcaPyApiKey: String?,
    @Value("\${issuer-verifier.acapy.url}") private val issuerVerifierAcaPyUrl: String?,
    @Value("\${issuer-verifier.acapy.http-timeout-in-seconds}") private val issuerVerifierAcaPyHttpTimeoutInSeconds: Long,
    @Value("\${issuer-verifier.multitenancy.enabled}") private val issuerVerifierMultitenancyEnabled: Boolean,
    @Value("\${issuer-verifier.multitenancy.wallet-type}") private val issuerVerifierMultitenancyWalletType: String,
    @Value("\${issuer-verifier.multitenancy.register-did-endpoint}") private val registerDidEndpoint: String,
    @Value("\${issuer-verifier.multitenancy.webhook-endpoint-url}") private val webhookEndpointUrl: String,
    @Value("\${issuer-verifier.multitenancy.sub-wallet-name}") private val subWalletName: String,
    @Value("\${holder.acapy.api-key}") private val holderAcaPyApiKey: String?,
    @Value("\${holder.acapy.urls}") private val holderAcaPyUrls: Array<String>,
    @Value("\${holder.acapy.http-timeout-in-seconds}") private val holderAcapyHttpTimeoutInSeconds: Long
) {
    var logger: Logger = LoggerFactory.getLogger(AcaPyConfig::class.java)

    @Bean(name = ["IssuerVerifier"])
    fun issuerVerifierAriesClient(okHttpPublisher: AcaPyOkHttpInterceptor): IAriesClient? {
        if (issuerVerifierAcaPyUrl == null) {
            logger.error("Unable to establish connection to Issuer/Verifier AcaPy. Issuer/Verifier AcaPy URL not configured.")
            return null
        }

        if (issuerVerifierMultitenancyEnabled) {
            return issuerVerifierClientWithMultitenancyEnabled(issuerVerifierAcaPyUrl, okHttpPublisher)
        }

        return issuerVerifierClientWithMultitenancyDisabled(issuerVerifierAcaPyUrl, okHttpPublisher)
    }

    private fun issuerVerifierClientWithMultitenancyDisabled(
        issuerVerifierAcaPyUrl: String,
        okHttpPublisher: AcaPyOkHttpInterceptor
    ): IAriesClient {
        val issuerVerifierAcaPyClient =
            buildAcaPyAriesClient(
                okHttpPublisher,
                issuerVerifierAcaPyUrl,
                issuerVerifierAcaPyHttpTimeoutInSeconds,
                issuerVerifierAcaPyApiKey,
                null
            )

        return AcaPyAriesClient(issuerVerifierAcaPyClient, errorLogger, ariesClientLogger)
    }

    private fun issuerVerifierClientWithMultitenancyEnabled(
        issuerVerifierAcaPyUrl: String,
        okHttpPublisher: AcaPyOkHttpInterceptor
    ): IAriesClient {
        val baseWalletAriesClient =
            buildAcaPyAriesClient(
                okHttpPublisher,
                issuerVerifierAcaPyUrl,
                issuerVerifierAcaPyHttpTimeoutInSeconds,
                issuerVerifierAcaPyApiKey,
                null
            )

        val subWalletToken = createNewSubWallet(baseWalletAriesClient)

        val subWalletIssuerVerifierAcaPyClient =
            buildAcaPyAriesClient(
                okHttpPublisher,
                issuerVerifierAcaPyUrl,
                issuerVerifierAcaPyHttpTimeoutInSeconds,
                issuerVerifierAcaPyApiKey,
                subWalletToken
            )

        createAndRegisterNewPublicDid(subWalletIssuerVerifierAcaPyClient)

        return AcaPyAriesClient(subWalletIssuerVerifierAcaPyClient, errorLogger, ariesClientLogger)
    }

    private fun createNewSubWallet(baseWalletAriesClient: AriesClient): String {
        val createWalletRequestBuilder = CreateWalletRequest.builder()
            .keyManagementMode(KeyManagementMode.MANAGED)
            .walletDispatchType(WalletDispatchType.DEFAULT)
            .walletKey("key")
            .walletName(subWalletName)
            .walletWebhookUrls(listOf(webhookEndpointUrl))


        when (issuerVerifierMultitenancyWalletType) {
            "askar" -> createWalletRequestBuilder.walletType(WalletType.ASKAR)
            "indy" -> createWalletRequestBuilder.walletType(WalletType.INDY)
            else -> logger.error("Unable to create sub wallet as wallet type $issuerVerifierMultitenancyWalletType is unknown.")
        }

        var walletRecord: Optional<WalletRecord>? = null

        try {
            walletRecord = baseWalletAriesClient.multitenancyWalletCreate(
                createWalletRequestBuilder.build()
            )
        } catch (e: Exception) {
        }

        if (walletRecord == null || walletRecord.isEmpty) {
            val errorMessage =
                "Unable to create a new sub wallet. Has the multitenancy mode been enabled for the AcaPy?"
            logger.error(errorMessage)
            throw Exception(errorMessage)
        }

        logger.info("Created Issuer/Verifier sub wallet with the name '$subWalletName' that is reporting to '$webhookEndpointUrl'.")

        return walletRecord.get().token
    }

    private fun createAndRegisterNewPublicDid(ariesClient: AriesClient) {
        val newDid = createNewDid(ariesClient)
        publishDidOnLedger(newDid.did, newDid.verkey)
        makeDidPublicDid(ariesClient, newDid.did)
    }

    private fun createNewDid(ariesClient: AriesClient): DID {
        val did = ariesClient.walletDidCreate(
            DIDCreate.builder()
                .method(DIDCreate.MethodEnum.SOV)
                .options(
                    DIDCreateOptions.builder()
                        .keyType(DIDCreateOptions.KeyTypeEnum.ED25519)
                        .build()
                )
                .build()
        )

        if (did.isEmpty) {
            logger.error("Unable to create a new DID.")
        }

        return did.get()
    }

    private fun publishDidOnLedger(did: String, verKey: String) {
        val rest = RestTemplate()

        val didRegistration = object {
            val did = did
            val verkey = verKey
            val role = "ENDORSER"
        }

        rest.postForObject<String>(registerDidEndpoint, didRegistration)
    }

    private fun makeDidPublicDid(ariesClient: AriesClient, did: String) {
        val result = ariesClient.walletDidPublic(did)

        if (result.isEmpty) {
            logger.error("Unable to make DID a public DID.")
        }
    }


    @Bean(name = ["Holder"])
    fun holderAriesClient(okHttpPublisher: AcaPyOkHttpInterceptor): List<IAriesClient> {
        val holderAcaPyClients = mutableListOf<IAriesClient>()

        if (holderAcaPyUrls.isEmpty()) {
            logger.error("Unable to establish connection to Holder AcaPy. Holder AcaPy URL not configured.")
            return holderAcaPyClients
        }

        logger.info("Using ${holderAcaPyUrls.size} Holder Agents")
        holderAcaPyUrls.forEach {
        logger.info("Using Holder Agent: $it")
            val holderAcaPyClient =
                buildAcaPyAriesClient(okHttpPublisher, it, holderAcapyHttpTimeoutInSeconds, holderAcaPyApiKey, null)

            holderAcaPyClients.add(
                AcaPyAriesClient(holderAcaPyClient, errorLogger, ariesClientLogger)
            )
        }

        return holderAcaPyClients
    }

    private fun buildAcaPyAriesClient(
        acaPyOkHttpInterceptor: AcaPyOkHttpInterceptor,
        acaPyUrl: String,
        acaPyHttpTimeoutInSeconds: Long,
        acaPyApiKey: String?,
        bearerToken: String?,
    ): AriesClient {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(acaPyOkHttpInterceptor)
            .writeTimeout(acaPyHttpTimeoutInSeconds, TimeUnit.SECONDS)
            .readTimeout(acaPyHttpTimeoutInSeconds, TimeUnit.SECONDS)
            .connectTimeout(acaPyHttpTimeoutInSeconds, TimeUnit.SECONDS)
            .callTimeout(acaPyHttpTimeoutInSeconds, TimeUnit.SECONDS)
            .build()

        return AriesClient.builder()
            .url(acaPyUrl)
            .apiKey(acaPyApiKey)
            .client(okHttpClient)
            .bearerToken(bearerToken)
            .build()
    }
}
