package com.bka.ssi.generator.application.testcases

import com.bka.ssi.generator.infrastructure.ariesevents.IAriesObserver

abstract class TestRunner(
) : IAriesObserver {

    abstract fun run()
}
