package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.enums.InstallmentStatus;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class RepaymentScheduleService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(RepaymentScheduleService.class);

    private final RepaymentScheduleRepository scheduleRepository;

    public RepaymentScheduleService(RepaymentScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public void generateSchedule(UUID loanId,
                                 BigDecimal principalAmount,
                                 BigDecimal interestRate,
                                 int tenureMonths,
                                 BigDecimal emiAmount,
                                 LocalDate firstDueDate) {

        spectraLogger.info("REPAYMENT_SCHEDULE_GENERATE_START")
                .attr("loanId", loanId)
                .attr("tenureMonths", tenureMonths)
                .attr("emiAmount", emiAmount)
                .log();

        BigDecimal monthlyInterestRate = interestRate.divide(
                BigDecimal.valueOf(12 * 100), 10, java.math.RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = principalAmount;
        LocalDate dueDate = firstDueDate;

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyInterestRate)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal principalDue = emiAmount.subtract(interestDue);
            if (i == tenureMonths) {
                principalDue = remainingPrincipal;
            }

            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loanId(loanId)
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(principalDue.add(interestDue))
                    .principalPaid(BigDecimal.ZERO)
                    .interestPaid(BigDecimal.ZERO)
                    .lateFeePaid(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .status(InstallmentStatus.PENDING)
                    .build();

            scheduleRepository.create(schedule);

            spectraLogger.info("REPAYMENT_SCHEDULE_INSTALLMENT_CREATED")
                    .attr("loanId", loanId)
                    .attr("installmentNumber", i)
                    .attr("principalDue", principalDue)
                    .attr("interestDue", interestDue)
                    .log();

            remainingPrincipal = remainingPrincipal.subtract(principalDue);
            dueDate = dueDate.plusMonths(1);
        }

        spectraLogger.info("REPAYMENT_SCHEDULE_GENERATE_COMPLETE")
                .attr("loanId", loanId)
                .attr("installments", tenureMonths)
                .log();
    }
}