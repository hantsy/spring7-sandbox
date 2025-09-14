package com.example.demo;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.hibernate.HibernateTransactionManager;

@Configuration
public class JakartaDataConfig {

    @Bean
    public StatelessSession statelessSession(SessionFactory sessionFactory) {
        return sessionFactory.openStatelessSession();
    }

    @Bean
    public SessionFactory sessionFactory(LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        return entityManagerFactoryBean.getObject().unwrap(SessionFactory.class);
    }

    // Hibernate Data Repositories registered spring bean automatically in the generated impl class.
    // @Bean
    // public JakartaDataPostRepository dataPostRepository(StatelessSession statelessSession) {
    //     return new JakartaDataPostRepository_(statelessSession);
    // }

    @Bean
    public HibernateTransactionManager hibernateTransactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

}