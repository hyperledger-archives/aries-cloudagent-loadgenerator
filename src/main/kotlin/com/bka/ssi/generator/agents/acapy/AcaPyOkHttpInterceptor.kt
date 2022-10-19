package com.bka.ssi.generator.agents.acapy

import com.bka.ssi.generator.application.logger.ErrorLogger
import com.bka.ssi.generator.domain.services.IHttpRequestObserver
import okhttp3.Interceptor
import okhttp3.Response

class AcaPyRequestLoggerInterceptor(
    private val ariesClientType: String,
    private val handler: IHttpRequestObserver,
    private val errorLogger: ErrorLogger
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()
        val response = chain.proceed(request)
        val t2 = System.nanoTime()

        val durationInMs = (t2 - t1) / 1e6

        handler.logHttpRequest(request.method, request.url.encodedPath, response.code, durationInMs)

        if (response.code != 200 && response.code != 201) {
            errorLogger.reportAriesClientHttpRequestError(
                ariesClientType = "AcaPy.$ariesClientType",
                httpMethod = request.method,
                uri = request.url.encodedPath,
                httpResponseCode = response.code,
                durationInMs = durationInMs,
                appErrorCode = "n/a",
                message = response.body?.string() ?: ""
            )
        }

        return response
    }
}
