package in.zeta.microloan.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {

                "in.zeta.microloan.platform"
        })
public class MicroloanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroloanPlatformApplication.class, args);
    }

}
