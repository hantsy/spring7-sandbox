package com.example.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebMvc
@ComponentScan(
        basePackageClasses = WebConfig.class,
        useDefaultFilters = false,
        includeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        value = {
                                Controller.class,
                                RestController.class,
                                ControllerAdvice.class
                        }
                )
        }
)
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    JsonMapper jsonMapper;

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        var httpMessageConverter = new JacksonJsonHttpMessageConverter(jsonMapper);
        builder.withJsonConverter(httpMessageConverter);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
    }

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("/WEB-INF/templates");

        return configurer;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/posts");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
}
