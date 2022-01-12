package com.bka.ssi.generator.application

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
        logger.info("New ConnectionRecord!")
    }

    override fun handleCredential(credential: V1CredentialExchange?) {
        logger.info("New CredentialExchangeRecord!")
    }

    override fun handleRevocation(revocation: RevocationEvent?) {
        logger.info("New ConnectionRevocationEvent!")
    }

    override fun handleProof(proof: PresentationExchangeRecord) {
        logger.info("New PresentationExchangeRecord!")
    }
}
