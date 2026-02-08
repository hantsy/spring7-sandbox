# Modularizing Spring Boot 4 Application

One breaking change in Spring Boot 4 is that the single monolithic `spring-boot-autoconfigure` module is split into smaller, fine-grained feature and library specific modules. Each feature or library now provides its own `starter` and `starter-test` modules, which allows developers to include only the autoconfiguration classes and third-party libraries their application requires.

## Transforming to Spring Boot 4

Go to [Spring Initializr](https://start.spring.io/) , generate a Spring Boot 3 project with given dependencies: 

* 