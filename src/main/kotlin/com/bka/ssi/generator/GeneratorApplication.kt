package com.bka.ssi.generator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GeneratorApplication

fun main(args: Array<String>) {
	runApplication<GeneratorApplication>(*args)
}
