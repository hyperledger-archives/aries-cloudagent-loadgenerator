package com.bka.ssi.generator.application.testcases

import org.hyperledger.aries.webhook.EventHandler

abstract class TestRunner(
) : EventHandler() {

    abstract fun run()
}
