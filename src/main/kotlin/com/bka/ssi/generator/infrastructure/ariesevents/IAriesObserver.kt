package com.bka.ssi.generator.infrastructure.ariesevents

import com.bka.ssi.generator.domain.ConnectionRecordDo
import com.bka.ssi.generator.domain.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.ProofExchangeRecordDo

interface IAriesObserver {
    fun handleConnectionRecord(connectionRecord: ConnectionRecordDo)
    fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo)
    fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo)
}
