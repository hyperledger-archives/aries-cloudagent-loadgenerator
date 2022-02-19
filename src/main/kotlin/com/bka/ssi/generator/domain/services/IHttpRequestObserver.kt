package com.bka.ssi.generator.domain.services

interface IHttpRequestObserver {
    fun logHttpRequest(httpMethod: String, urlPath: String, httpResponseCode: Int, durationInMs: Double)
}
