package com.example.demo

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions

@Configuration
@Import(MyBeanRegistrar::class)
class CustomConfig

class MyBeanRegistrar : BeanRegistrarDsl({
    registerBean { InMemoryPostRepository() }
    registerBean { PostHandler(bean()) }
    registerBean { Routes(bean()) }

    registerBean(
        name = "webHandler",
        backgroundInit = true,
        prototype = false,
        supplier = {
            RouterFunctions.toWebHandler(bean<Routes>().routes(), HandlerStrategies.builder().build())
        }
    )
    register(FoobarRegistrar())
})

class FoobarRegistrar : BeanRegistrarDsl({
    profile("foo") {
        registerBean<Bar> { Bar(bean<Foo>()) }
        registerBean { Foo() }
    }
})

class Bar(foo: Foo)
class Foo