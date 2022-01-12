package com.bka.ssi.generator

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

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
}
