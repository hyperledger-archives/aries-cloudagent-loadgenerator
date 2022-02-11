package com.bka.ssi.generator

import com.bka.ssi.generator.application.testrunners.TestRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GeneratorApplication {

	@Bean
	fun runTest(testRunner: TestRunner): CommandLineRunner {
		return CommandLineRunner { _ ->
			testRunner.run()
		}
	}

}

fun main(args: Array<String>) {
	runApplication<GeneratorApplication>(*args)
}
