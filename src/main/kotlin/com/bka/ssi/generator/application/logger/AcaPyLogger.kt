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

package com.bka.ssi.generator.application.logger

import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord
import org.hyperledger.aries.api.revocation.RevocationEvent
import org.hyperledger.aries.webhook.EventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AcaPyLogger : EventHandler() {
    var logger: Logger = LoggerFactory.getLogger(AcaPyLogger::class.java)

    override fun handleConnection(connection: ConnectionRecord?) {
        logger.info(
                "Connection: time: ${connection?.updatedAt}, Connection-Id: ${connection?.connectionId}, state: ${connection?.state}"
        )
    }

    override fun handleCredential(credential: V1CredentialExchange?) {
        logger.info(
                "Credential: time: ${credential?.updatedAt}, Cred-Ex-Id: ${credential?.credentialExchangeId}, Connection-Id: ${credential?.connectionId}, state: ${credential?.state}"
        )
    }

    override fun handleRevocation(revocation: RevocationEvent?) {
        logger.info(
                "Revocation: time: ${revocation?.createdAt}, Rev-Reg-Id: ${revocation?.revRegId}, Cred-Def-Id: ${revocation?.credDefId}, Cred-Ex-Id: ${revocation?.credExId}, state: ${revocation?.state}"
        )
    }

    override fun handleProof(proof: PresentationExchangeRecord?) {
        logger.info(
                "Presentation: time: ${proof?.updatedAt}, Presentation-Ex-Id: ${proof?.presentationExchangeId}, Connection-Id: ${proof?.connectionId}, state: ${proof?.state}"
        )
    }
}
