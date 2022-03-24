package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.application.logger.ErrorLogger
import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.objects.ProofExchangeCommentDo
import com.bka.ssi.generator.domain.objects.ProofExchangeRecordDo
import com.bka.ssi.generator.domain.services.IAriesObserver
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord
import org.hyperledger.aries.api.present_proof.PresentationExchangeState
import org.hyperledger.aries.webhook.EventHandler
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat


@Service
class AcaPyPublisher(
    private val handlers: List<IAriesObserver>,
    private val errorLogger: ErrorLogger
) : EventHandler() {

    private fun dateStringToMilliseconds(date: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return dateFormat.parse(date.dropLast(4)).getTime()
    }

    override fun handleConnection(connection: ConnectionRecord) {
        if (connection.errorMsg != null) {
            errorLogger.reportAriesEventError("AcaPyPublisher.handleConnection: ${connection.errorMsg}")
            return
        }

        handlers.forEach {
            it.handleConnectionRecord(
                ConnectionRecordDo(
                    connection.connectionId,
                    dateStringToMilliseconds(connection.updatedAt),
                    connection.state.toString(),
                    connection.stateIsActive()
                )
            )
        }
    }

    override fun handleProof(proof: PresentationExchangeRecord) {
        if (proof.errorMsg != null) {
            errorLogger.reportAriesEventError("AcaPyPublisher.handleProof: ${proof.errorMsg}")
            return
        }

        handlers.forEach {
            it.handleProofRequestRecord(
                ProofExchangeRecordDo(
                    proof.presentationExchangeId,
                    dateStringToMilliseconds(proof.updatedAt),
                    proof.connectionId,
                    proof.state.toString(),
                    proof.state == PresentationExchangeState.VERIFIED,
                    proof.isVerified,
                    ProofExchangeCommentDo(proof.presentationRequest.name)
                )
            )
        }
    }

    override fun handleCredential(credential: V1CredentialExchange) {
        if (credential.errorMsg != null) {
            errorLogger.reportAriesEventError("AcaPyPublisher.handleCredential: ${credential.errorMsg}")
            return
        }

        handlers.forEach {
            it.handleCredentialExchangeRecord(
                CredentialExchangeRecordDo(
                    credential.credentialExchangeId,
                    credential.credentialOfferDict.credentialPreview.attributes[0].value,
                    credential.connectionId,
                    dateStringToMilliseconds(credential.updatedAt),
                    credential.state.toString(),
                    credential.stateIsCredentialAcked(),
                    credential.revocRegId,
                    credential.revocationId
                )
            )
        }
    }
}
