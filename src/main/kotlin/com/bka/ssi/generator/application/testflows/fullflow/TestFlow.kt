package com.bka.ssi.generator.application.testflows.fullflow

import com.bka.ssi.generator.application.testrunners.TestRunner
import com.bka.ssi.generator.domain.services.IAriesObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class TestFlow(

) : IAriesObserver {

    protected var logger: Logger = LoggerFactory.getLogger(TestFlow::class.java)

    abstract fun initialize(testRunner: TestRunner)
    abstract fun startIteration()
}
