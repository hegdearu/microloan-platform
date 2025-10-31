package in.zeta.microloan.platform.config;

import in.zeta.springframework.boot.commons.postgres.GenericPostgresDAO;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

@Configuration
public class DaoConfig {

    @Bean
    public GenericPostgresDAO getGenericPostgresDAO(@Value("${postgres.jdbc.pool.size}") int poolSize,
                                                    BasicDataSource basicDataSource) {
        return new GenericPostgresDAO(basicDataSource, poolSize);
    }

    @Bean
    public BasicDataSource getBasicDataSource(@Value("${postgres.jdbc.url}") String url,
                                              @Value("${postgres.jdbc.username}") String username,
                                              @Value("${postgres.jdbc.password}") String password,
                                              @Value("${postgres.jdbc.driver}") String driver,
                                              @Value("${postgres.jdbc.time.between.eviction.runs.millis}") long timeBetweenEvictionRunsMillis,
                                              @Value("${postgres.jdbc.pool.size}") int poolSize) {
        checkNotNull(driver, "PG driver can't be null");
        checkNotNull(url, "DB url can't be null");

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(url);
        basicDataSource.setMaxTotal(poolSize);
        basicDataSource.setMaxIdle(poolSize);
        basicDataSource.setPoolPreparedStatements(true);
        basicDataSource.setDurationBetweenEvictionRuns(Duration.ofSeconds(timeBetweenEvictionRunsMillis));
        basicDataSource.setTestOnBorrow(false);
        basicDataSource.setFastFailValidation(true);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setDriverClassName(driver);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);

        return basicDataSource;
    }
}
