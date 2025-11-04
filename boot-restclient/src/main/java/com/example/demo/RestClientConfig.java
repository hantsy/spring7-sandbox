package com.example.demo;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    RestClientCustomizer restClientCustomizer(JsonMapper mapper) {
        return builder -> builder
                .baseUrl("http://localhost:9090")
                .configureMessageConverters(c -> c.registerDefaults()
                        .jsonMessageConverter(new JacksonJsonHttpMessageConverter(mapper))
                )
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    @Bean
    ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder() {
        return ClientHttpRequestFactoryBuilder.jdk();
//                .withCustomizer()
//                .withHttpClientCustomizer()
//                .withExecutor()
    }

//    @Bean
//    ClientHttpRequestFactoryBuilderCustomizer<JdkClientHttpRequestFactoryBuilder> clientHttpRequestFactoryCustomizer(){
//        return builder -> builder.
//    }
//

}
