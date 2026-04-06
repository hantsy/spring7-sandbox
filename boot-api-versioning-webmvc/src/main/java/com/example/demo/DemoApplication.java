package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
@RequestMapping("/hello")
class GreetingController {

    @GetMapping()
    public String helloDefault() {
        return "Hello v1.0(Default)";
    }

    @GetMapping(version = "1.1")
    public String helloV1_1() {
        return "Hello v1.1";
    }

    @GetMapping(version = "2.0")
    public String helloV2_0() {
        return "Hello v2.0";
    }
}

@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                // Add version in uri path: /api/v1/resource
                // .usePathSegment(0)

                // Set in HTTP header: X-API-Version: 1
                .useRequestHeader("X-API-Version")

                // Add a request parameter: /api/resource?version=1
                // .useRequestParam("version")

                // Append version parameter to `Accept` header value: application/json;version=1
                // .useMediaTypeParameter(MediaType.APPLICATION_JSON, "version" )

                .addSupportedVersions("1.0", "1.1", "2.0")

                // default is SemanticApiVersionParser
                //.setVersionParser(new SemanticApiVersionParser())

                // When a defaultVersion is also set, this is automatically set to false.
                // .setVersionRequired(true)
                .setDefaultVersion("1.0");
    }
}
