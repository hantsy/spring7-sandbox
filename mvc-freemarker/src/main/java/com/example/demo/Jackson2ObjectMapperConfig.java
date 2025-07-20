package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class Jackson2ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {

        var builder = JsonMapper.builder();

        builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .findAndAddModules();
// Jackson2ObjectMapperBuilder is removed, due to the Jackson 3 JsonMapperBuilder.
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
