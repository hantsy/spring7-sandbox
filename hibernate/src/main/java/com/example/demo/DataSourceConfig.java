package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@PropertySource(value = "classpath:/datasource.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {

    private static final String ENV_DATASOURCE_PASSWORD = "datasource.password";
    private static final String ENV_DATASOURCE_USERNAME = "datasource.username";
    private static final String ENV_DATASOURCE_URL = "datasource.url";
    private static final String ENV_DATASOURCE_DRIVER_CLASS_NAME ="datasource.driverClassName";
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    //private static final String ENV_DATASOURCE_JNDINAME = "datasource.jndi-name";

    @Autowired
    private Environment env;

    @Bean
    public DataSource defaultDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(env.getProperty(ENV_DATASOURCE_URL));
        dataSource.setUsername(env.getProperty(ENV_DATASOURCE_USERNAME));
        dataSource.setPassword(env.getProperty(ENV_DATASOURCE_PASSWORD));

        if(env.getProperty(ENV_DATASOURCE_DRIVER_CLASS_NAME) != null){
            log.debug("DataSource Driver class name:{}", env.getProperty(ENV_DATASOURCE_DRIVER_CLASS_NAME));
            dataSource.setDriverClassName(env.getProperty(ENV_DATASOURCE_DRIVER_CLASS_NAME));
        }

        log.debug("Default DataSource:: url={}, username={}, password={}", dataSource.getUrl(),  dataSource.getUsername(), dataSource.getPassword());
        return dataSource;
    }

}