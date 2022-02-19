package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.objects.ProofExchangeRecordDo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * This flow is used to debug the Load Generator.
 * (e.g. to better understand the behaviour of the ScheduledThreadPool)
 */
@Service
@ConditionalOnProperty(
    name = ["test-flows.empty-flow.active"],
    matchIfMissing = false
)
class EmptyFlow(
) : TestFlow(
    emptyList()
) {

    protected companion object {
        var testRunner: TestRunner? = null
    }

    override fun initialize(testRunner: TestRunner) {
        logger.info("Initializing EmptyFlow...")

        Companion.testRunner = testRunner

        testRunner.finishedInitialization()
    }

    override fun startIteration() {
        logger.info("Iteration started.")

        Thread.sleep(2000)

        logger.info("Iteration finished.")
        testRunner?.finishedIteration()
    }

    override fun handleConnectionRecord(connectionRecord: ConnectionRecordDo) {
        throw NotImplementedError("Not supported by Empty Flow.")
    }

    override fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo) {
        throw NotImplementedError("Not supported by Empty Flow.")
    }

    override fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo) {
        throw NotImplementedError("Not supported by Empty Flow.")
    }
}
