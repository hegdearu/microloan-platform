package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.RepaymentDTO;
import in.zeta.microloan.platform.dto.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.repository.RepaymentRepository;
import in.zeta.microloan.platform.repository.RepaymentScheduleRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    private final AtroposEventPublisherService atroposEventPublisher;

    public RepaymentService(RepaymentRepository repaymentRepository,
                            RepaymentScheduleRepository scheduleRepository,
                            LoanRepository loanRepository, AtroposEventPublisherService atroposEventPublisher) {
        this.repaymentRepository = repaymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.loanRepository = loanRepository;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Transactional
    public RepaymentResponseDTO recordRepayment(RepaymentDTO dto, Long createdBy) {
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!loan.getStatus().name().equals("ACTIVE") &&
                !loan.getStatus().name().equals("OVERDUE")) {
            throw new BusinessRuleException("Repayment can only be made for active or overdue loans");
        }

        List<RepaymentSchedule> pendingSchedules =
                scheduleRepository.findPendingByLoanId(dto.getLoanId());

        if (pendingSchedules.isEmpty()) {
            throw new BusinessRuleException("No pending installments found");
        }

        BigDecimal remainingAmount = dto.getAmount();
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        BigDecimal totalLateFeePaid = BigDecimal.ZERO;

        for (RepaymentSchedule schedule : pendingSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal principalDue = schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid());
            BigDecimal interestDue = schedule.getInterestDue().subtract(schedule.getInterestPaid());

            // Calculate late fee if overdue
            BigDecimal lateFee = BigDecimal.ZERO;
            if (LocalDate.now().isAfter(schedule.getDueDate().plusDays(loan.getGracePeriodDays()))) {
                lateFee = schedule.getTotalDue().multiply(loan.getLateFeePercent())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            }

            BigDecimal lateFeeDue = lateFee.subtract(schedule.getLateFeePaid());
            BigDecimal totalDue = principalDue.add(interestDue).add(lateFeeDue);

            if (remainingAmount.compareTo(totalDue) >= 0) {
                // Full payment of this installment
                totalPrincipalPaid = totalPrincipalPaid.add(principalDue);
                totalInterestPaid = totalInterestPaid.add(interestDue);
                totalLateFeePaid = totalLateFeePaid.add(lateFeeDue);
                remainingAmount = remainingAmount.subtract(totalDue);

                scheduleRepository.updatePayment(
                        schedule.getId(),
                        principalDue,
                        interestDue,
                        lateFeeDue,
                        "PAID",
                        LocalDate.now()
                );
            } else {
                // Partial payment - prioritize interest, then late fee, then principal
                BigDecimal interestPayment = remainingAmount.min(interestDue);
                remainingAmount = remainingAmount.subtract(interestPayment);
                totalInterestPaid = totalInterestPaid.add(interestPayment);

                BigDecimal lateFeePayment = remainingAmount.min(lateFeeDue);
                remainingAmount = remainingAmount.subtract(lateFeePayment);
                totalLateFeePaid = totalLateFeePaid.add(lateFeePayment);

                BigDecimal principalPayment = remainingAmount.min(principalDue);
                remainingAmount = remainingAmount.subtract(principalPayment);
                totalPrincipalPaid = totalPrincipalPaid.add(principalPayment);

                scheduleRepository.updatePayment(
                        schedule.getId(),
                        principalPayment,
                        interestPayment,
                        lateFeePayment,
                        "PARTIALLY_PAID",
                        null
                );

                break; // Stop processing further installments
            }
        }

        // Record the repayment
        Repayment repayment = Repayment.builder()
                .receiptNumber(generateReceiptNumber())
                .loanId(dto.getLoanId())
                .borrowerId(loan.getBorrowerId())
                .householdId(loan.getHouseholdId())
                .amount(dto.getAmount())
                .principalPaid(totalPrincipalPaid)
                .interestPaid(totalInterestPaid)
                .lateFeePaid(totalLateFeePaid)
                .advancePayment(remainingAmount)
                .paymentDate(dto.getPaymentDate())
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod()))
                .transactionRef(dto.getTransactionRef())
                .notes(dto.getNotes())
                .status(PaymentStatus.COMPLETED)
                .createdBy(createdBy)
                .build();

        Long repaymentId = repaymentRepository.create(repayment);
        repayment.setId(repaymentId);

        // Update loan outstanding amounts
        loanRepository.updateOutstanding(
                dto.getLoanId(),
                totalPrincipalPaid,
                totalInterestPaid
        );
        loanRepository.updateTotalPaid(dto.getLoanId(), dto.getAmount());

        // Check if loan is fully paid
        Loan updatedLoan = loanRepository.findById(dto.getLoanId()).get();
        if (updatedLoan.getTotalOutstanding().compareTo(BigDecimal.ZERO) <= 0) {
            loanRepository.updateStatus(dto.getLoanId(), "CLOSED");

            // Publish loan closed event
            Loan closedLoan = loanRepository.findById(dto.getLoanId()).get();
            atroposEventPublisher.publishLoanClosedEvent(closedLoan);
        } else if (updatedLoan.getStatus().name().equals("OVERDUE")) {
            // Check if no more overdue installments
            List<RepaymentSchedule> stillOverdue = pendingSchedules.stream()
                    .filter(s -> s.getStatus().name().equals("OVERDUE"))
                    .collect(Collectors.toList());
            if (stillOverdue.isEmpty()) {
                loanRepository.updateStatus(dto.getLoanId(), "ACTIVE");
            }
        }

        atroposEventPublisher.publishLoanRepaymentEvent(repayment, loan);

        return mapToResponseDTO(repayment);
    }

    public List<RepaymentResponseDTO> getRepaymentsByLoan(Long loanId) {
        List<Repayment> repayments = repaymentRepository.findByLoanId(loanId);
        return repayments.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getRepaymentSchedule(Long loanId) {
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanId(loanId);
        return schedules.stream()
                .map(this::mapToScheduleResponseDTO)
                .collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getPendingSchedule(Long loanId) {
        List<RepaymentSchedule> pending = scheduleRepository.findPendingByLoanId(loanId);
        return pending.stream()
                .map(this::mapToScheduleResponseDTO)
                .collect(Collectors.toList());
    }

    private String generateReceiptNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "RCP-" + datePart + "-" + randomPart;
    }

    private RepaymentResponseDTO mapToResponseDTO(Repayment repayment) {
        return RepaymentResponseDTO.builder()
                .id(repayment.getId())
                .receiptNumber(repayment.getReceiptNumber())
                .loanId(repayment.getLoanId())
                .amount(repayment.getAmount())
                .principalPaid(repayment.getPrincipalPaid())
                .interestPaid(repayment.getInterestPaid())
                .lateFeePaid(repayment.getLateFeePaid())
                .advancePayment(repayment.getAdvancePayment())
                .paymentDate(repayment.getPaymentDate())
                .paymentMethod(repayment.getPaymentMethod().name())
                .transactionRef(repayment.getTransactionRef())
                .status(repayment.getStatus().name())
                .createdAt(repayment.getCreatedAt())
                .build();
    }

    private RepaymentScheduleResponseDTO mapToScheduleResponseDTO(RepaymentSchedule schedule) {
        return RepaymentScheduleResponseDTO.builder()
                .id(schedule.getId())
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .principalPaid(schedule.getPrincipalPaid())
                .interestPaid(schedule.getInterestPaid())
                .lateFeePaid(schedule.getLateFeePaid())
                .totalPaid(schedule.getTotalPaid())
                .status(schedule.getStatus().name())
                .paidDate(schedule.getPaidDate())
                .build();
    }
}
