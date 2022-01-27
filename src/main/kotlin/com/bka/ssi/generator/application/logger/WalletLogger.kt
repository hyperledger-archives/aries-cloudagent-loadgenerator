package com.bka.ssi.generator.application.logger

import com.bka.ssi.generator.domain.services.IWallet
import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WalletLogger(var wallet: IWallet) {
    var logger: Logger = LoggerFactory.getLogger(WalletLogger::class.java)

    @Scheduled(fixedRate = 10000)
    fun reportCurrentTime() {
        logger.info(
                "type=database_size Current size of the issuer/verifier database in bytes: size=${wallet.walletDatabaseSizeInBytes()}, time=${
                Instant.now().toEpochMilli()
            }"
        )
    }
}
