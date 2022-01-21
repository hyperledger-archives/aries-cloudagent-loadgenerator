package com.bka.ssi.generator.domain.services

import com.bka.ssi.generator.domain.objects.ConnectionRecordDo
import com.bka.ssi.generator.domain.objects.CredentialExchangeRecordDo
import com.bka.ssi.generator.domain.objects.ProofExchangeRecordDo

interface IAriesObserver {
    fun handleConnectionRecord(connectionRecord: ConnectionRecordDo)
    fun handleCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecordDo)
    fun handleProofRequestRecord(proofExchangeRecord: ProofExchangeRecordDo)
}
