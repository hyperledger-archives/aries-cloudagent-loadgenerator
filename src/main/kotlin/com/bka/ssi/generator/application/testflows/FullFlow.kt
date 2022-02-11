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

package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.*
import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant


@Service
@ConditionalOnProperty(
    name = ["test-flows.full-flow.active"],
    matchIfMissing = false
)
class FullFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") private val holderAriesClient: IAriesClient,
    @Value("\${test-flows.full-flow.use-connectionless-proof-requests}") private val useConnectionlessProofRequests: Boolean
) : TestFlow() {

    protected companion object {
        var credentialDefinitionId = ""
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing test flow...")
        Companion.testRunner = testRunner

        val credentialDefinition = issuerVerifierAriesClient.createSchemaAndCredentialDefinition(
            SchemaDo(
                listOf("first name", "last name"),
                "name",
                "1.0"
            )
        )

        credentialDefinitionId = credentialDefinition.id

        logger.info("Initialization completed.")
    }

    override fun startIteration() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        try {
            holderAriesClient.receiveConnectionInvitation(connectionInvitation)
        } catch (exception: Exception) {
            logger.error("${exception.message} (Connection Invitation: ${connectionInvitation.toString()})")
            testRunner?.finishedIteration()
            return
        }
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        if (!connectionRecord.active) {
            return
        }

        issuerVerifierAriesClient.issueCredentialToConnection(
            connectionRecord.connectionId,
            CredentialDo(
                credentialDefinitionId,
                mapOf(
                    "first name" to "Holder",
                    "last name" to "Mustermann"
                )
            )
        )

        logger.info("Issued credential to new connection")
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        if (!credentialExchangeRecord.issued) {
            return
        }

        if (useConnectionlessProofRequests) {
            val connectionLessProofRequest = issuerVerifierAriesClient.createConnectionlessProofRequest(
                ProofRequestDo(
                    Instant.now().toEpochMilli(),
                    Instant.now().toEpochMilli(),
                    listOf(
                        CredentialRequestDo(
                            listOf("first name", "last name"),
                            credentialDefinitionId
                        )
                    )
                )
            )

            holderAriesClient.receiveConnectionlessProofRequest(connectionLessProofRequest)
        } else {
            issuerVerifierAriesClient.sendProofRequestToConnection(
                credentialExchangeRecord.connectionId,
                ProofRequestDo(
                    Instant.now().toEpochMilli(),
                    Instant.now().toEpochMilli(),
                    listOf(
                        CredentialRequestDo(
                            listOf("first name", "last name"),
                            credentialDefinitionId
                        )
                    )
                )
            )
        }

        logger.info("Send proof request")
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        if (!proofExchangeRecord.verifiedAndValid) {
            return
        }

        logger.info("Received valid proof presentation")

        testRunner?.finishedIteration()
    }
}
