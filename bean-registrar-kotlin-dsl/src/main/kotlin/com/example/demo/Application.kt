package com.example.demo

import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer

class Application {
    fun main(args: Array<String>) {
        val context = AnnotationConfigApplicationContext()
        context.register(CustomConfig::class.java)
        context.refresh()
        runBlocking { context.getBean(PostRepository::class.java).initializeSampleData() }
        nettyServer(context).onDispose().block()

    }

    fun nettyServer(context: ApplicationContext): DisposableServer {
        val handler = WebHttpHandlerBuilder.applicationContext(context).build()
        val adapter = ReactorHttpHandlerAdapter(handler)

        val httpServer = HttpServer.create().host("localhost").port(8080)
        return httpServer.handle(adapter).bindNow()
    }
}

fun main(args: Array<String>) {
    Application().main(args)
}
