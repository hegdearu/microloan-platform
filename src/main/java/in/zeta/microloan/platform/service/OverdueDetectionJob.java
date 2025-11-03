package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.enums.CollectionStage;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.OverdueTracking;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.repository.overduetracking.OverdueTrackingRepository;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class OverdueDetectionJob {

    private static final Logger logger = LoggerFactory.getLogger(OverdueDetectionJob.class);

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final OverdueTrackingRepository overdueRepository;
    private final AtroposEventPublisherService atroposEventPublisher;

    public OverdueDetectionJob(LoanRepository loanRepository,
                               RepaymentScheduleRepository scheduleRepository,
                               OverdueTrackingRepository overdueRepository, AtroposEventPublisherService atroposEventPublisher) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.overdueRepository = overdueRepository;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void detectOverdueLoans() {
        logger.info("Starting overdue detection job...");

        try {
            List<Loan> activeLoans = loanRepository.findByStatus("ACTIVE");
            int overdueCount = 0;

            for (Loan loan : activeLoans) {
                List<RepaymentSchedule> schedules = scheduleRepository.findByLoanId(loan.getId());

                RepaymentSchedule firstOverdueInstallment = null;
                for (RepaymentSchedule schedule : schedules) {
                    if ("PENDING".equals(schedule.getStatus())) {
                        LocalDate gracePeriodEnd = schedule.getDueDate().plusDays(loan.getGracePeriodDays());

                        if (LocalDate.now().isAfter(gracePeriodEnd)) {
                            scheduleRepository.updateStatus(schedule.getId(), "OVERDUE");
                            if (firstOverdueInstallment == null) {
                                firstOverdueInstallment = schedule;
                            }
                        }
                    }
                }

                if (firstOverdueInstallment != null) {
                    loanRepository.updateStatus(loan.getId(), "OVERDUE");

                    LocalDate overdueSince = firstOverdueInstallment.getDueDate()
                            .plusDays(loan.getGracePeriodDays() + 1);
                    int overdueDays = (int) ChronoUnit.DAYS.between(overdueSince, LocalDate.now());

                    BigDecimal overduePrincipal = BigDecimal.ZERO;
                    BigDecimal overdueInterest = BigDecimal.ZERO;

                    for (RepaymentSchedule schedule : schedules) {
                        if ("OVERDUE".equals(schedule.getStatus())) {
                            overduePrincipal = overduePrincipal.add(
                                    schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid()));
                            overdueInterest = overdueInterest.add(
                                    schedule.getInterestDue().subtract(schedule.getInterestPaid()));
                        }
                    }

                    BigDecimal overdueAmount = overduePrincipal.add(overdueInterest);

                    BigDecimal penaltyAmount = overdueAmount
                            .multiply(loan.getLateFeePercent())
                            .multiply(BigDecimal.valueOf(overdueDays))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    BigDecimal maxPenalty = loan.getEmiAmount()
                            .multiply(BigDecimal.valueOf(50))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    if (penaltyAmount.compareTo(maxPenalty) > 0) {
                        penaltyAmount = maxPenalty;
                    }

                    BigDecimal totalDue = overdueAmount.add(penaltyAmount);

                    String collectionStage = determineCollectionStage(overdueDays);

                    BigDecimal finalOverduePrincipal = overduePrincipal;
                    BigDecimal finalOverdueInterest = overdueInterest;
                    BigDecimal finalPenaltyAmount = penaltyAmount;
                    BigDecimal finalOverdueAmount = overdueAmount;
                    BigDecimal finalTotalDue = totalDue;
                    CollectionStage finalCollectionStage = CollectionStage.valueOf(collectionStage);
                    LocalDate finalOverdueSince = overdueSince;
                    overdueRepository.findByLoanId(loan.getId()).ifPresentOrElse(
                            existing -> {
                                existing.setOverdueDays(overdueDays);
                                existing.setOverduePrincipal(finalOverduePrincipal);
                                existing.setOverdueInterest(finalOverdueInterest);
                                existing.setOverdueAmount(finalOverdueAmount);
                                existing.setPenaltyAmount(finalPenaltyAmount);
                                existing.setTotalDue(finalTotalDue);
                                existing.setLastCheckedAt(LocalDateTime.now());
                                existing.setCollectionStage(finalCollectionStage);
                                overdueRepository.update(existing);
                                atroposEventPublisher.publishLoanOverdueEvent(loan, existing);
                            },
                            () -> {
                                OverdueTracking tracking = OverdueTracking.builder()
                                        .loanId(loan.getId())
                                        .overdueSince(finalOverdueSince)
                                        .overdueDays(overdueDays)
                                        .overduePrincipal(finalOverduePrincipal)
                                        .overdueInterest(finalOverdueInterest)
                                        .overdueAmount(finalOverdueAmount)
                                        .penaltyAmount(finalPenaltyAmount)
                                        .totalDue(finalTotalDue)
                                        .lastCheckedAt(LocalDateTime.now())
                                        .collectionStage(finalCollectionStage)
                                        .build();
                                overdueRepository.create(tracking);
                                atroposEventPublisher.publishLoanOverdueEvent(loan, tracking);
                            }
                    );

                    overdueCount++;
                    logger.info("Loan {} marked as overdue", loan.getLoanNumber());
                }
            }

            logger.info("Overdue detection completed. {} loans processed", overdueCount);

        } catch (Exception e) {
            logger.error("Error in overdue detection job", e);
        }
    }

    private String determineCollectionStage(int overdueDays) {
        if (overdueDays <= 7) return "SOFT_REMINDER";
        if (overdueDays <= 30) return "REGULAR_FOLLOWUP";
        if (overdueDays <= 60) return "STRICT_FOLLOWUP";
        return "LEGAL_NOTICE";
    }
}
