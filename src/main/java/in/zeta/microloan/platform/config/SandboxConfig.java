package in.zeta.microloan.platform.config;

import in.zeta.microloan.platform.provider.UserProvider;
import in.zeta.springframework.boot.commons.authorization.sandboxAccessControl.SandboxAccessControlProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SandboxConfig {

    @Bean
    @Primary
    public SandboxAccessControlProvider getSandboxAccessControlProvider(
            UserProvider userProvider,
            SandboxAccessControlProvider sacp
    ) {
        sacp.registerObjectProvider(UserProvider.OBJECT_TYPE, userProvider);
        return sacp;
    }
}
