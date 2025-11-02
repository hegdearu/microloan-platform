package in.zeta.microloan.platform.config;

import in.zeta.microloan.platform.provider.LoanProvider;
import in.zeta.microloan.platform.provider.RepaymentProvider;
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
            LoanProvider loanProvider,
            RepaymentProvider repaymentProvider,
            SandboxAccessControlProvider sacp
    ) {
        sacp.registerObjectProvider(UserProvider.OBJECT_TYPE, userProvider);
        sacp.registerObjectProvider(LoanProvider.OBJECT_TYPE, loanProvider);
        sacp.registerObjectProvider(RepaymentProvider.OBJECT_TYPE, repaymentProvider);
        return sacp;
    }
}
