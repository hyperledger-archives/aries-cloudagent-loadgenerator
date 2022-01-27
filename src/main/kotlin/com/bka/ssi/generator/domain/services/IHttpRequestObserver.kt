package com.bka.ssi.generator.domain.services

interface IHttpRequestObserver {
    fun handleHttpRequest(httpMethod: String, urlPath: String, httpResponseCode: Int, durationInMs: Double)
}
