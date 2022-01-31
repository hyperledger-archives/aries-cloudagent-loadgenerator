package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
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
    private val handlers: List<IAriesObserver>
) : EventHandler() {

    private fun dateStringToMilliseconds(date: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return dateFormat.parse(date.dropLast(4)).getTime()
    }

    override fun handleConnection(connection: ConnectionRecord) {
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
        handlers.forEach {
            it.handleProofRequestRecord(
                ProofExchangeRecordDo(
                    proof.presentationExchangeId,
                    dateStringToMilliseconds(proof.updatedAt),
                    proof.connectionId,
                    proof.state.toString(),
                    proof.isVerified && proof.state == PresentationExchangeState.VERIFIED
                )
            )
        }
    }

    override fun handleCredential(credential: V1CredentialExchange) {
        handlers.forEach {
            it.handleCredentialExchangeRecord(
                CredentialExchangeRecordDo(
                    credential.credentialExchangeId,
                    credential.connectionId,
                    dateStringToMilliseconds(credential.updatedAt),
                    credential.state.toString(),
                    credential.stateIsCredentialAcked()
                )
            )
        }
    }
}
