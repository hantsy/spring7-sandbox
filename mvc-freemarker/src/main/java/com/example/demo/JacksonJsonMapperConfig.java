package com.example.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonJsonMapperConfig {

    @Bean
    public JsonMapper jsonMapper() {

        var builder = JsonMapper.builder();

        builder.changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .findAndAddModules();
// Spring Jackson2ObjectMapperBuilder is deprecated, due to the Jackson 3 JsonMapper.Builder.
//        var builder = Jackson2ObjectMapperBuilder.json();
//        builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
//        builder.featuresToDisable(
//                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
//                SerializationFeature.FAIL_ON_EMPTY_BEANS,
//                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
//                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
//        builder.modulesToInstall(JavaTimeModule.class);

        return builder.build();
    }
}
