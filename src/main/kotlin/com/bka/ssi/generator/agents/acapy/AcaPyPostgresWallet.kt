package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.domain.services.IWallet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service


@Service
class AcaPyPostgresWallet(
    val jdbcTemplate: JdbcTemplate,
    @Value("\${issuer-verifier.wallet-db-name}") val walletDbName: String
) : IWallet {
    var logger: Logger = LoggerFactory.getLogger(AcaPyPostgresWallet::class.java)

    override fun walletDatabaseSizeInBytes(): Int? {
        val size = jdbcTemplate.queryForObject(
            "SELECT PG_DATABASE_SIZE('$walletDbName')",
            Int::class.java
        )

        return size
    }
}
