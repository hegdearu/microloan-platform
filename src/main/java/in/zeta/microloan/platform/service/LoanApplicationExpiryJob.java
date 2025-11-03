package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoanApplicationExpiryJob {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanApplicationExpiryJob.class);
    private final LoanApplicationRepository applicationRepository;

    public LoanApplicationExpiryJob(LoanApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    // Runs daily at 00:10
    @Scheduled(cron = "0 10 0 * * ?")
    @Transactional
    public void markExpiredApplications() {
        spectraLogger.info("LOAN_APPLICATION_EXPIRY_JOB_START").log();
        try {
            int affected = applicationRepository.expirePendingApplications();
            spectraLogger.info("LOAN_APPLICATION_EXPIRY_JOB_RESULT")
                    .attr("expiredCount", affected)
                    .log();
        } catch (Exception e) {
            spectraLogger.error("LOAN_APPLICATION_EXPIRY_JOB_ERROR")
                    .attr("error", e.getMessage())
                    .log();
        } finally {
            spectraLogger.info("LOAN_APPLICATION_EXPIRY_JOB_END").log();
        }
    }
}
