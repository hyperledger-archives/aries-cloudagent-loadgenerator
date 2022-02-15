package com.bka.ssi.generator.application.testflows

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.services.IAriesClient
import com.bka.ssi.generator.domain.services.IAriesObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class TestFlow(
    private var holderAriesClients: List<IAriesClient>
) : IAriesObserver {

    private companion object {
        var indexOfNextHolder = 0
    }

    protected var logger: Logger = LoggerFactory.getLogger(TestFlow::class.java)

    abstract fun initialize(testRunner: TestRunner)
    abstract fun startIteration()

    protected fun nextHolderClient(): IAriesClient {
        val holderClient = holderAriesClients[indexOfNextHolder]

        indexOfNextHolder++
        if (indexOfNextHolder == holderAriesClients.size) indexOfNextHolder = 0

        return holderClient
    }
}
