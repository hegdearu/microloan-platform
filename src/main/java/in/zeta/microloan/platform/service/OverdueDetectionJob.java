package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.enums.CollectionStage;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.OverdueTracking;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.repository.overduetracking.OverdueTrackingRepository;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class OverdueDetectionJob {
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final OverdueTrackingRepository overdueRepository;
    private final AtroposEventPublisherService atroposEventPublisher;

    public OverdueDetectionJob(LoanRepository loanRepository,
                               RepaymentScheduleRepository scheduleRepository,
                               OverdueTrackingRepository overdueRepository,
                               AtroposEventPublisherService atroposEventPublisher) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.overdueRepository = overdueRepository;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void detectOverdueLoans() {
        try {
            List<Loan> activeLoans = loanRepository.findByStatus("ACTIVE");
            int overdueCount = 0;

            for (Loan loan : activeLoans) {
                List<RepaymentSchedule> schedules = scheduleRepository.findByLoanId(loan.getId());

                RepaymentSchedule firstOverdueInstallment = null;
                for (RepaymentSchedule schedule : schedules) {
                    if ("PENDING".equals(schedule.getStatus())) {
                        LocalDate graceEnd = schedule.getDueDate().plusDays(loan.getGracePeriodDays());
                        if (LocalDate.now().isAfter(graceEnd)) {
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

                    BigDecimal rawPenalty = overdueAmount
                            .multiply(loan.getLateFeePercent())
                            .multiply(BigDecimal.valueOf(overdueDays))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    BigDecimal maxPenalty = loan.getEmiAmount()
                            .multiply(BigDecimal.valueOf(50))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    BigDecimal penaltyAmount = rawPenalty.compareTo(maxPenalty) > 0 ? maxPenalty : rawPenalty;

                    BigDecimal totalDue = overdueAmount.add(penaltyAmount);
                    String collectionStage = determineCollectionStage(overdueDays);

                    Optional<OverdueTracking> opt = overdueRepository.findByLoanId(loan.getId());
                    OverdueTracking overdueTracking;
                    if (opt.isPresent()) {
                        OverdueTracking existing = opt.get();
                        existing.setOverdueDays(overdueDays);
                        existing.setOverduePrincipal(overduePrincipal);
                        existing.setOverdueInterest(overdueInterest);
                        existing.setOverdueAmount(overdueAmount);
                        existing.setPenaltyAmount(penaltyAmount);
                        existing.setTotalDue(totalDue);
                        existing.setCollectionStage(CollectionStage.valueOf(collectionStage));
                        existing.setUpdatedAt(LocalDateTime.now());
                        overdueRepository.update(existing);
                        overdueTracking = existing;

                    } else {
                        OverdueTracking tracking = OverdueTracking.builder()
                                .loanId(loan.getId())
                                .overdueDays(overdueDays)
                                .overduePrincipal(overduePrincipal)
                                .overdueInterest(overdueInterest)
                                .overdueAmount(overdueAmount)
                                .penaltyAmount(penaltyAmount)
                                .totalDue(totalDue)
                                .collectionStage(CollectionStage.valueOf(collectionStage))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        overdueRepository.create(tracking);
                        overdueTracking = tracking;

                    }

                    atroposEventPublisher.publishLoanOverdueEvent(loan, overdueTracking);
                    overdueCount++;
                }
            }

        } catch (Exception e) {

        }
    }

    private String determineCollectionStage(int overdueDays) {
        if (overdueDays <= 7) return "SOFT_REMINDER";
        if (overdueDays <= 30) return "REGULAR_FOLLOWUP";
        if (overdueDays <= 60) return "STRICT_FOLLOWUP";
        return "LEGAL_NOTICE";
    }
}