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

import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.objects.ProofExchangeRecordDo
import com.bka.ssi.generator.domain.services.IAriesObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AriesEventLogger : IAriesObserver {
    var logger: Logger = LoggerFactory.getLogger(AriesEventLogger::class.java)

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        logger.info(
                "type=connection_record time=${connectionRecord.time} Connection-Id=${connectionRecord.connectionId} state=${connectionRecord.state}"
        )
    }

    override fun handleCredentialExchangeRecord(
            credentialExchangeRecord: CredentialExchangeRecordDo
    ) {
        logger.info(
            "type=credential_record time=${credentialExchangeRecord.time} Cred-Ex-Id=${credentialExchangeRecord.id} Connection-Id=${credentialExchangeRecord.connectionId} state=${credentialExchangeRecord.state}"
        )
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        logger.info(
            "type=presentation_record time=${proofExchangeRecord.time} Presentation-Ex-Id=${proofExchangeRecord.id} Connection-Id=${proofExchangeRecord.connectionId} state=${proofExchangeRecord.state}"
        )
    }
}
