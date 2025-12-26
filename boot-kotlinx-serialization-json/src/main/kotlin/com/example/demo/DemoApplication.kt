package com.example.demo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNamingStrategy
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.kotlinx.serialization.json.autoconfigure.KotlinxSerializationJsonBuilderCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@OptIn(ExperimentalSerializationApi::class)
@Configuration(proxyBeanMethods = false)
class CustomConfig {

    @Bean
    fun kotlinxSerializationJsonBuilderCustomizer(): KotlinxSerializationJsonBuilderCustomizer {
        return KotlinxSerializationJsonBuilderCustomizer { builder ->
            builder.apply {
                namingStrategy = JsonNamingStrategy.SnakeCase
                prettyPrint = true
                explicitNulls = false
                encodeDefaults = true
                ignoreUnknownKeys = false
            }
        }
    }

}