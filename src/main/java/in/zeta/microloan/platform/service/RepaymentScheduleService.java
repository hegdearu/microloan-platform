package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.InstallmentStatus;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.repository.RepaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RepaymentScheduleService {

    private final RepaymentScheduleRepository scheduleRepository;

    public RepaymentScheduleService(RepaymentScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public void generateSchedule(Long loanId, BigDecimal principalAmount,
                                 BigDecimal interestRate, int tenureMonths,
                                 BigDecimal emiAmount, LocalDate firstDueDate) {

        BigDecimal monthlyInterestRate = interestRate.divide(
                BigDecimal.valueOf(12 * 100), 10, java.math.RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = principalAmount;
        LocalDate dueDate = firstDueDate;

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyInterestRate)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            BigDecimal principalDue = emiAmount.subtract(interestDue);

            // For last installment, adjust for any rounding differences
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

            remainingPrincipal = remainingPrincipal.subtract(principalDue);
            dueDate = dueDate.plusMonths(1);
        }
    }
}
