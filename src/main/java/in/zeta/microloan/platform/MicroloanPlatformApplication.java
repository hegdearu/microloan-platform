package in.zeta.microloan.platform;

import in.zeta.springframework.boot.commons.pubsub.ZetaRESTConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = {"in.zeta.springframework.boot.commons",
                "in.zeta.microloan.platform"
        })
public class MicroloanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroloanPlatformApplication.class, args);
    }

}
