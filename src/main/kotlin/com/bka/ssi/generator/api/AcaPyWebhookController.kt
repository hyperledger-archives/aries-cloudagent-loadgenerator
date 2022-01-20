package com.bka.ssi.generator.api

import org.hyperledger.aries.webhook.EventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/webhook")
class AcaPyWebhookController(
    private val handler: EventHandler
) {

    var logger: Logger = LoggerFactory.getLogger(AcaPyWebhookController::class.java)

    @PostMapping("/topic/{topic}")
    fun ariesEvent(
        @PathVariable topic: String?,
        @RequestBody message: String?
    ) {
        handler.handleEvent(topic, message)
    }
}
