package com.bka.ssi.generator.application.testrunners

import com.bka.ssi.generator.application.testflows.TestFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["test-runners.max-parallel-iterations-runner.active"],
    matchIfMissing = false
)
class MaxParallelIterationsTestRunner(
    private val testFlow: TestFlow,
    @Value("\${test-runners.max-parallel-iterations-runner.number-of-total-iterations}") val numberOfTotalIterations: Int,
    @Value("\${test-runners.max-parallel-iterations-runner.number-of-parallel-iterations}") val numberOfParallelIterations: Int,
) : TestRunner(
) {

    protected companion object {
        var numberOfIterationsStarted = 0
        var numberOfIterationsFinished = 0
    }

    override fun run() {
        testFlow.initialize(this)
    }

    override fun finishedInitialization() {
        logger.info("Starting MaxParallelIterationsTestRunner...")
        logger.info("Number of Iterations: $numberOfTotalIterations")
        logger.info("Number of Parallel Iterations: $numberOfParallelIterations")

        for (i in 0 until numberOfParallelIterations) {
            numberOfIterationsStarted++
            logger.info("Started $numberOfIterationsStarted of $numberOfTotalIterations iterations")
            testFlow.startIteration()
        }
    }

    override fun finishedIteration() {
        numberOfIterationsFinished++
        logger.info("Finished ${numberOfIterationsFinished} of $numberOfTotalIterations iterations")

        if (numberOfIterationsFinished < numberOfTotalIterations) {
            testFlow.startIteration()
        }
    }
}
