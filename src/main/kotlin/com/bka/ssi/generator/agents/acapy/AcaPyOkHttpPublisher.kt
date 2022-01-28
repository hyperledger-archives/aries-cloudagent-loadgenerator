package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.domain.services.IHttpRequestObserver
import okhttp3.Interceptor
import okhttp3.Response
import org.springframework.stereotype.Service

@Service
class OkHttpPublisher(
    private val handler: IHttpRequestObserver
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val t1 = System.nanoTime()
        val response = chain.proceed(request)
        val t2 = System.nanoTime()

        val durationInMs = (t2 - t1) / 1e6

        handler.handleHttpRequest(request.method, request.url.encodedPath, response.code, durationInMs)

        return response
    }
}
