package com.bka.ssi.generator.api

import com.bka.ssi.generator.agents.acapy.AcaPyPublisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@RestController
@ApiIgnore
@RequestMapping("/acapy-webhook")
class AcaPyWebhookController(
    private val handler: AcaPyPublisher
) {

    var logger: Logger = LoggerFactory.getLogger(AcaPyWebhookController::class.java)

    @PostMapping("/topic/{topic}")
    fun ariesEvent(@PathVariable topic: String?, @RequestBody message: String?) {
        handler.handleEvent(topic, message)
    }
}
