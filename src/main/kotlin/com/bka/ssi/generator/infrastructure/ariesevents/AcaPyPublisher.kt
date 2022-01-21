package com.bka.ssi.generator.infrastructure.ariesevents

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

    var dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'")

    override fun handleConnection(connection: ConnectionRecord) {
        handlers.forEach {
            it.handleConnectionRecord(
                ConnectionRecordDo(
                    connection.connectionId,
                    dateFormat.parse(connection.updatedAt).getTime(),
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
                    dateFormat.parse(proof.updatedAt).getTime(),
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
                    dateFormat.parse(credential.updatedAt).getTime(),
                    credential.state.toString(),
                    credential.stateIsCredentialAcked()
                )
            )
        }
    }
}
