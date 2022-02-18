package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.objects.ProofExchangeRecordDo
import com.bka.ssi.generator.domain.services.IAriesClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service


@Service
@ConditionalOnProperty(
    name = ["test-flows.connection-flow.active"],
    matchIfMissing = false
)
class ConnectionFlow(
    @Qualifier("IssuerVerifier") private val issuerVerifierAriesClient: IAriesClient,
    @Qualifier("Holder") holderAriesClients: List<IAriesClient>,
) : TestFlow(holderAriesClients) {

    protected companion object {
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing ConnectionFlow...")

        Companion.testRunner = testRunner

        testRunner.finishedInitialization()
    }

    override fun startIteration() {
        val connectionInvitation = issuerVerifierAriesClient.createConnectionInvitation("holder-acapy")

        nextHolderClient().receiveConnectionInvitation(connectionInvitation)
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        if (!connectionRecord.active) {
            return
        }

        logger.info("Established new connection")

        testRunner?.finishedIteration()
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        throw NotImplementedError("The connection flow does not handle credential exchange records.")
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        throw NotImplementedError("The connection flow does not handle proof exchange records.")
    }
}
