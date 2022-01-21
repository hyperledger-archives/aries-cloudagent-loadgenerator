package com.bka.ssi.generator.application.testcases

import com.bka.ssi.generator.domain.services.IAriesObserver

abstract class TestRunner(
) : IAriesObserver {

    abstract fun run()
}
