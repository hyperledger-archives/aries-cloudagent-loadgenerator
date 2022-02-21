package com.bka.ssi.generator.application.testrunners

import com.bka.ssi.generator.application.logger.ErrorLogger
import com.bka.ssi.generator.application.testflows.TestFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Service
@ConditionalOnProperty(
    name = ["test-runners.constant-load-runner.active"],
    matchIfMissing = false
)
class ConstantLoadTestRunner(
    private val testFlow: TestFlow,
    private val errorLogger: ErrorLogger,
    @Value("\${test-runners.constant-load-runner.number-of-total-iterations}") val numberOfTotalIterations: Int,
    @Value("\${test-runners.constant-load-runner.number-of-iterations-per-minute}") val numberOfIterationsPerMinute: Int,
    @Value("\${test-runners.constant-load-runner.thread-pool-size}") val threadPoolSize: Int
) : TestRunner(
) {

    lateinit var loadScheduler: ScheduledFuture<*>
    lateinit var fixedThreadPoolExecutor: ExecutorService

    protected companion object {
        var numberOfIterationsStarted = 0
        var numberOfIterationsFinished = 0
    }

    override fun run() {
        testFlow.initialize(this)
    }

    override fun finishedInitialization() {
        logger.info("Starting ConstantLoadTestRunner...")
        logger.info("Number of Iterations: $numberOfTotalIterations")
        logger.info("Number of Iterations per Minute: $numberOfIterationsPerMinute")
        logger.info("Thread pool size: $threadPoolSize")
        logger.info("Expected running duration in minutes: ${numberOfTotalIterations / numberOfIterationsPerMinute}")

        fixedThreadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize)

        val executor = Executors.newScheduledThreadPool(4)
        loadScheduler = executor.scheduleAtFixedRate(
            Runnable {
                try {
                    fixedThreadPoolExecutor.submit { startNewIteration() }
                } catch (exception: Exception) {
                    errorLogger.reportTestRunnerError("The 'loadScheduler' of the 'ConstantLoadTestRunner' caught an error: ${exception.message} [${exception.printStackTrace()}]")
                    exception.printStackTrace();
                }
            },
            0,
            60000L / numberOfIterationsPerMinute,
            TimeUnit.MILLISECONDS
        )
    }

    private fun startNewIteration() {
        numberOfIterationsStarted++
        logger.info("Started $numberOfIterationsStarted of $numberOfTotalIterations iterations")
        testFlow.startIteration()
    }

    override fun finishedIteration() {
        numberOfIterationsFinished++
        logger.info("Finished ${numberOfIterationsFinished} of $numberOfTotalIterations iterations")

        if (numberOfIterationsFinished >= numberOfTotalIterations) {
            loadScheduler.cancel(true)
            fixedThreadPoolExecutor.shutdownNow()
        }
    }

}
