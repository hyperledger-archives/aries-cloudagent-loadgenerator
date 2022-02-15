package com.bka.ssi.generator.application.testrunners

import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class TestRunner {

    protected var logger: Logger = LoggerFactory.getLogger(TestRunner::class.java)

    abstract fun run()
    abstract fun finishedInitialization()
    abstract fun finishedIteration()
}
