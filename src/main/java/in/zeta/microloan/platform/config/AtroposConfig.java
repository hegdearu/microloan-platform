package in.zeta.microloan.platform.config;

import in.zeta.oms.atropos.client.AtroposPublisherClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtroposConfig {
    @Bean
    public AtroposPublisherClient atroposPublisherClient() {
        return new AtroposPublisherClient();
    }
}