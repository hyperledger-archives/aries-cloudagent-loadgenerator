package com.bka.ssi.generator.config

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType
import org.springframework.boot.actuate.endpoint.ExposableEndpoint
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver
import org.springframework.boot.actuate.endpoint.web.EndpointMapping
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.util.StringUtils
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Tag
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*


/**
 * Build a Swagger2 configuration file
 */
@Configuration
@EnableSwagger2
class SwaggerConfig {

    private val apiPackage = "com.bka.ssi.generator.api"
    private val title = "API Documentation for AcaPy Load Generator"
    private val description = "This is an automatically generated RESTfull API documentation and UI."
    private val version = "v1.0"

    @Bean
    fun createRestApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .tags(Tag("info", "Endpoints to get information"))
            .select()
            .apis(RequestHandlerSelectors.basePackage(apiPackage))
            .paths(PathSelectors.any())
            .build()
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title(title)
            .description(description)
            .version(version)
            .build()
    }

    // This bean resolves problem with actuator and swagger
    // https://github.com/springfox/springfox/issues/3462#issuecomment-1010721223
    @Bean
    fun webEndpointServletHandlerMapping(
        webEndpointsSupplier: WebEndpointsSupplier,
        servletEndpointsSupplier: ServletEndpointsSupplier,
        controllerEndpointsSupplier: ControllerEndpointsSupplier,
        endpointMediaTypes: EndpointMediaTypes?,
        corsProperties: CorsEndpointProperties,
        webEndpointProperties: WebEndpointProperties,
        environment: Environment
    ): WebMvcEndpointHandlerMapping? {
        val allEndpoints: MutableList<ExposableEndpoint<*>?> = ArrayList()
        val webEndpoints = webEndpointsSupplier.endpoints
        allEndpoints.addAll(webEndpoints)
        allEndpoints.addAll(servletEndpointsSupplier.endpoints)
        allEndpoints.addAll(controllerEndpointsSupplier.endpoints)
        val basePath = webEndpointProperties.basePath
        val endpointMapping = EndpointMapping(basePath)
        val shouldRegisterLinksMapping = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath)
        return WebMvcEndpointHandlerMapping(
            endpointMapping,
            webEndpoints,
            endpointMediaTypes,
            corsProperties.toCorsConfiguration(),
            EndpointLinksResolver(allEndpoints, basePath),
            shouldRegisterLinksMapping,
            null
        )
    }

    private fun shouldRegisterLinksMapping(
        webEndpointProperties: WebEndpointProperties,
        environment: Environment,
        basePath: String
    ): Boolean {
        return webEndpointProperties.discovery.isEnabled && (StringUtils.hasText(basePath) || ManagementPortType.get(
            environment
        ) == ManagementPortType.DIFFERENT)
    }
}
