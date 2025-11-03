package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.PaymentMethod;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
import in.zeta.microloan.platform.repository.repayment.RepaymentRepository;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RepaymentService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(RepaymentService.class);

    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    private final AtroposEventPublisherService atroposEventPublisher;

    public RepaymentService(RepaymentRepository repaymentRepository,
                            RepaymentScheduleRepository scheduleRepository,
                            LoanRepository loanRepository,
                            AtroposEventPublisherService atroposEventPublisher) {
        this.repaymentRepository = repaymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.loanRepository = loanRepository;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Transactional
    public RepaymentResponseDTO recordRepayment(RepaymentRequestDTO dto, Long createdBy) {
        spectraLogger.info("REPAYMENT_PROCESS_START")
                .attr("loanId", dto.getLoanId())
                .attr("amount", dto.getAmount())
                .attr("paymentDate", dto.getPaymentDate())
                .attr("paymentMethod", dto.getPaymentMethod())
                .log();

        // --- Validate loan existence & status ---
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!(loan.getStatus().name().equals("ACTIVE") || loan.getStatus().name().equals("OVERDUE"))) {
            spectraLogger.warn("REPAYMENT_INVALID_LOAN_STATUS")
                    .attr("loanId", dto.getLoanId())
                    .attr("status", loan.getStatus())
                    .log();
            throw new BusinessRuleException("Repayment can only be made for active or overdue loans");
        }

        // --- Fetch pending installments ---
        List<RepaymentSchedule> pendingSchedules = scheduleRepository.findPendingByLoanId(dto.getLoanId());
        if (pendingSchedules.isEmpty()) {
            spectraLogger.warn("REPAYMENT_NO_PENDING_INSTALLMENTS")
                    .attr("loanId", dto.getLoanId())
                    .log();
            throw new BusinessRuleException("No pending installments found");
        }

        BigDecimal remainingAmount = dto.getAmount();
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        BigDecimal totalLateFeePaid = BigDecimal.ZERO;

        for (RepaymentSchedule schedule : pendingSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal principalDue = schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid());
            BigDecimal interestDue = schedule.getInterestDue().subtract(schedule.getInterestPaid());

            // Calculate late fee if applicable
            BigDecimal lateFeeDue = BigDecimal.ZERO;
            if (LocalDate.now().isAfter(schedule.getDueDate().plusDays(loan.getGracePeriodDays()))) {
                BigDecimal lateFee = schedule.getTotalDue()
                        .multiply(loan.getLateFeePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                lateFeeDue = lateFee.subtract(schedule.getLateFeePaid());
            }

            BigDecimal totalDue = principalDue.add(interestDue).add(lateFeeDue);

            if (remainingAmount.compareTo(totalDue) >= 0) {
                remainingAmount = remainingAmount.subtract(totalDue);

                totalPrincipalPaid = totalPrincipalPaid.add(principalDue);
                totalInterestPaid  = totalInterestPaid.add(interestDue);
                totalLateFeePaid   = totalLateFeePaid.add(lateFeeDue);

                scheduleRepository.updatePayment(
                        schedule.getId(),
                        principalDue,
                        interestDue,
                        lateFeeDue,
                        "PAID",
                        LocalDate.now()
                );

                spectraLogger.info("INSTALLMENT_PAID")
                        .attr("scheduleId", schedule.getId())
                        .attr("principalPaid", principalDue)
                        .attr("interestPaid", interestDue)
                        .attr("lateFeePaid", lateFeeDue)
                        .log();
            } else {
                // ⚙️ Partial payment — priority: Late Fee → Interest → Principal
                BigDecimal lateFeePayment = remainingAmount.min(lateFeeDue);
                remainingAmount = remainingAmount.subtract(lateFeePayment);
                totalLateFeePaid = totalLateFeePaid.add(lateFeePayment);

                BigDecimal interestPayment = remainingAmount.min(interestDue);
                remainingAmount = remainingAmount.subtract(interestPayment);
                totalInterestPaid = totalInterestPaid.add(interestPayment);

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

                spectraLogger.info("INSTALLMENT_PARTIAL")
                        .attr("scheduleId", schedule.getId())
                        .attr("principalPaid", principalPayment)
                        .attr("interestPaid", interestPayment)
                        .attr("lateFeePaid", lateFeePayment)
                        .log();

                break;
            }
        }

        // --- Prevent negative advance ---
        BigDecimal advancePayment = remainingAmount.max(BigDecimal.ZERO);

        // --- Sanity check ---
        BigDecimal totalAllocated = totalPrincipalPaid.add(totalInterestPaid).add(totalLateFeePaid);
        if (totalAllocated.compareTo(dto.getAmount()) > 0) {
            throw new BusinessRuleException("Over-allocation detected: total components exceed payment amount");
        }

        String message = null;
        if (advancePayment.compareTo(BigDecimal.ZERO) > 0) {
            // Re-fetch remaining pending installments after updates
            List<RepaymentSchedule> remainingPending = scheduleRepository.findPendingByLoanId(dto.getLoanId());
            if (remainingPending.isEmpty()) {
                message = "Your credit balance is ₹ " + advancePayment.setScale(2) + ".";
            } else {
                message = "Advance Payment Balance: ₹ " + advancePayment.setScale(2) + " (will adjust against next EMI).";
            }
        }

        Repayment repayment = Repayment.builder()
                .receiptNumber(generateReceiptNumber())
                .loanId(dto.getLoanId())
                .borrowerId(loan.getBorrowerId())
                .householdId(loan.getHouseholdId())
                .amount(dto.getAmount())
                .principalPaid(totalPrincipalPaid)
                .interestPaid(totalInterestPaid)
                .lateFeePaid(totalLateFeePaid)
                .advancePayment(advancePayment)
                .paymentDate(dto.getPaymentDate())
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod()))
                .transactionRef(dto.getTransactionRef())
                .notes(dto.getNotes())
                .status(PaymentStatus.COMPLETED)
                .createdBy(createdBy)
                .build();

        UUID repaymentId = repaymentRepository.create(repayment);
        repayment.setId(repaymentId);

        loanRepository.updateOutstanding(dto.getLoanId(), totalPrincipalPaid, totalInterestPaid);
        loanRepository.updateTotalPaid(dto.getLoanId(), dto.getAmount());

        Loan updatedLoan = loanRepository.findById(dto.getLoanId()).get();
        if (updatedLoan.getTotalOutstanding().compareTo(BigDecimal.ZERO) <= 0) {
            loanRepository.updateStatus(dto.getLoanId(), "CLOSED");
            Loan closedLoan = loanRepository.findById(dto.getLoanId()).get();
            atroposEventPublisher.publishLoanClosedEvent(closedLoan);
            spectraLogger.info("REPAYMENT_LOAN_CLOSED")
                    .attr("loanId", dto.getLoanId())
                    .log();
        } else if (updatedLoan.getStatus().name().equals("OVERDUE")) {
            List<RepaymentSchedule> stillOverdue = scheduleRepository.findPendingByLoanId(dto.getLoanId()).stream()
                    .filter(s -> LocalDate.now().isAfter(s.getDueDate().plusDays(loan.getGracePeriodDays())))
                    .collect(java.util.stream.Collectors.toList());
            if (stillOverdue.isEmpty()) {
                loanRepository.updateStatus(dto.getLoanId(), "ACTIVE");
                spectraLogger.info("REPAYMENT_LOAN_STATUS_NORMALIZED")
                        .attr("loanId", dto.getLoanId())
                        .log();
            }
        }

        atroposEventPublisher.publishLoanRepaymentEvent(repayment, loan);

        spectraLogger.info("REPAYMENT_PROCESS_COMPLETE")
                .attr("repaymentId", repayment.getId())
                .attr("loanId", dto.getLoanId())
                .attr("principalPaidTotal", totalPrincipalPaid)
                .attr("interestPaidTotal", totalInterestPaid)
                .attr("lateFeePaidTotal", totalLateFeePaid)
                .attr("advanceRemaining", advancePayment)
                .attr("message", message)
                .log();

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
                .message(message)
                .build();
    }


    public List<RepaymentResponseDTO> getRepaymentsByLoan(UUID loanId) {
        List<Repayment> repayments = repaymentRepository.findByLoanId(loanId);
        return repayments.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getRepaymentSchedule(UUID loanId) {
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanId(loanId);
        return schedules.stream().map(this::mapToScheduleResponseDTO).collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getPendingSchedule(UUID loanId) {
        List<RepaymentSchedule> pending = scheduleRepository.findPendingByLoanId(loanId);
        return pending.stream().map(this::mapToScheduleResponseDTO).collect(Collectors.toList());
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