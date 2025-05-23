package com.example.demo;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.WebHandler;

@Configuration()
@Import(MyBeanRegistrar.class)
public class CustomConfig {

//    @Bean
//    public BeanRegistrar beanRegistrar() {
//        return new MyBeanRegistrar();
//    }

}

class MyBeanRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        if (env.matchesProfiles("h2")) {
            // registry.registerBean(R2dbcConfig.class);
            registry.registerBean(
                    PostRepository.class,
                    (BeanRegistry.Spec<PostRepository> spec) -> spec
                            .supplier(ctx -> new H2PostRepository(ctx.bean(DatabaseClient.class)))
            );
        } else {
            registry.registerBean(
                    PostRepository.class,
                    (BeanRegistry.Spec<PostRepository> spec) -> spec
                            .supplier(ctx -> new InMemoryPostRepository())
            );
        }
        registry.registerBean(PostHandler.class,
                (BeanRegistry.Spec<PostHandler> spec) -> spec
                        .supplier(ctx -> new PostHandler(ctx.bean(PostRepository.class)))
        );

        registry.registerBean(Routes.class,
                (BeanRegistry.Spec<Routes> spec) -> spec
                        .supplier(ctx -> new Routes(ctx.bean(PostHandler.class))));

        registry.registerBean("webHandler", WebHandler.class,
                (BeanRegistry.Spec<WebHandler> spec) -> spec
                        .prototype()
                        .supplier(ctx -> RouterFunctions.toWebHandler(ctx.bean(Routes.class).routes(), HandlerStrategies.builder().build()))
        );

    }
}
