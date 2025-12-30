package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;

@JsonTest
class JacksonTests {
    private static final Logger log = LoggerFactory.getLogger(JacksonTests.class);

    @Autowired
    JacksonTester<Person> tester;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void testPersonSerializationAndDeserialization() throws IOException {
        var data = new Person(
                "Hantsy Bai",
                LocalDate.of(1970, 1, 1),
                Gender.MALE
        );

        var jsonData = jsonMapper.writeValueAsString(data);
        log.debug("serialized json string: {} ", jsonData);

        tester.parse(jsonData)
                .assertThat()
                .matches(person -> person.name().equals("Hantsy Bai"));
    }

    @Test
    void deserializedWithFullName() throws IOException {
        var jsonData = """
                {
                    "fullName":"Hantsy Bai",
                    "birthDate":"01/01/1970",
                    "gender":"MALE"
                }
                """.trim();

        tester.parse(jsonData)
                .assertThat()
                .hasFieldOrProperty("name")
                .matches(person -> person.name().equals("Hantsy Bai"));

    }
}
