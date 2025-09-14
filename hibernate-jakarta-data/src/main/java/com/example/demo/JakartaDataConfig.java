package com.example.demo;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.hibernate.HibernateTransactionManager;
import org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:/jpa.properties", ignoreResourceNotFound = true)
public class JakartaDataConfig {

    public static final Logger log = LoggerFactory.getLogger(JakartaDataConfig.class);

    private static final String ENV_HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String ENV_HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
    private static final String ENV_HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String ENV_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";

    @Autowired
    Environment env;

    @Bean
    LocalSessionFactoryBean sessionFactoryBean(DataSource dataSource) {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("com.example.demo");
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }

    private Properties hibernateProperties() {
        Properties extraProperties = new Properties();
        extraProperties.put(ENV_HIBERNATE_FORMAT_SQL, env.getProperty(ENV_HIBERNATE_FORMAT_SQL));
        extraProperties.put(ENV_HIBERNATE_SHOW_SQL, env.getProperty(ENV_HIBERNATE_SHOW_SQL));
        extraProperties.put(ENV_HIBERNATE_HBM2DDL_AUTO, env.getProperty(ENV_HIBERNATE_HBM2DDL_AUTO));

        if (env.getProperty(ENV_HIBERNATE_DIALECT) != null) {
            log.debug("Hibernate Dialect: {}", env.getProperty(ENV_HIBERNATE_DIALECT));
            extraProperties.put(ENV_HIBERNATE_DIALECT, env.getProperty(ENV_HIBERNATE_DIALECT));
        }

        return extraProperties;
    }

//    @Bean(destroyMethod = "close")
//    public StatelessSession statelessSession(SessionFactory sessionFactory) {
//        return sessionFactory.openStatelessSession();
//    }
//
//    @Bean
//    public SessionFactory sessionFactory(LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
//        return entityManagerFactoryBean.getObject().unwrap(SessionFactory.class);
//    }
//
//     Hibernate Data Repositories registered spring bean automatically in the generated impl class.
//     @Bean
//     public JakartaDataPostRepository dataPostRepository(StatelessSession statelessSession) {
//         return new JakartaDataPostRepository_(statelessSession);
//     }

    @Bean
    public HibernateTransactionManager hibernateTransactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

}