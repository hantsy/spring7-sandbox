package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

@Configuration
class MyJacksonConfiguration {
    @Bean
    JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder
                .changeDefaultPropertyInclusion(value -> value.withContentInclusion(Include.NON_NULL))
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }
}

record Person(
        String name,
        LocalDate birthDate,
        Gender gender
) {
}

enum Gender {
    MALE,
    FEMALE
}

@JacksonComponent
class CustomLocalDateSerializer {

    static DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static class LocalDateSerializer extends ValueSerializer<LocalDate> {

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(value.format(LOCAL_DATE_FORMAT));
        }
    }

    static class LocalDateDeserializer extends ValueDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            return LocalDate.parse(ctxt.readValue(p, String.class), LOCAL_DATE_FORMAT);
        }
    }
}

@JacksonMixin(Person.class)
record MyPerson(@JsonProperty("fullName") String name) {
}