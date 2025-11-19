package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoanApplicationExpiryJob {
    private final LoanApplicationRepository applicationRepository;

    public LoanApplicationExpiryJob(LoanApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    // Runs daily at 00:10
    @Scheduled(cron = "0 10 0 * * ?")
    @Transactional
    public void markExpiredApplications() {
        try {
            int affected = applicationRepository.expirePendingApplications();
        } catch (Exception e) {

        } finally {
        }
    }
}
