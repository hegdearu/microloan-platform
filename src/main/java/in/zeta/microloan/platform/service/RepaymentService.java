package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.Repayment;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.model.enums.PaymentMethod;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.repayment.RepaymentRepository;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import in.zeta.microloan.platform.service.mappers.RepaymentMapper;
import in.zeta.microloan.platform.service.mappers.RepaymentScheduleMapper;
import in.zeta.microloan.platform.service.validator.RepaymentValidator;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static in.zeta.microloan.platform.exception.Error.LOAN_NOT_FOUND;

@Service
public class RepaymentService {

    private static final SpectraLogger log = OlympusSpectra.getLogger(RepaymentService.class);

    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    private final AtroposEventPublisherService atroposEventPublisher;
    private final RepaymentValidator repaymentValidator;
    private final RepaymentMapper repaymentMapper;
    private final RepaymentScheduleMapper scheduleMapper;

    public RepaymentService(RepaymentRepository repaymentRepository,
                            RepaymentScheduleRepository scheduleRepository,
                            LoanRepository loanRepository,
                            AtroposEventPublisherService atroposEventPublisher,
                            RepaymentValidator repaymentValidator,
                            RepaymentMapper repaymentMapper,
                            RepaymentScheduleMapper scheduleMapper) {
        this.repaymentRepository = repaymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.loanRepository = loanRepository;
        this.atroposEventPublisher = atroposEventPublisher;
        this.repaymentValidator = repaymentValidator;
        this.repaymentMapper = repaymentMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public RepaymentResponseDTO recordRepayment(RepaymentRequestDTO dto, Long createdBy) {
        log.info("REPAYMENT_PROCESS_START")
                .attr("loanId", dto.getLoanId())
                .attr("amount", dto.getAmount())
                .attr("paymentDate", dto.getPaymentDate())
                .attr("paymentMethod", dto.getPaymentMethod())
                .log();

        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_NOT_FOUND));

        List<RepaymentSchedule> pendingSchedules = scheduleRepository.findPendingByLoanId(dto.getLoanId());
        if (pendingSchedules.isEmpty()) {
            log.warn("REPAYMENT_NO_PENDING_INSTALLMENTS").attr("loanId", dto.getLoanId()).log();
            throw new BusinessRuleException("No pending installments found");
        }

        repaymentValidator.validateRecord(dto, loan, pendingSchedules);

        BigDecimal remainingAmount = dto.getAmount();
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        BigDecimal totalLateFeePaid = BigDecimal.ZERO;

        for (RepaymentSchedule schedule : pendingSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal principalDue = schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid());
            BigDecimal interestDue = schedule.getInterestDue().subtract(schedule.getInterestPaid());

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
                totalInterestPaid = totalInterestPaid.add(interestDue);
                totalLateFeePaid = totalLateFeePaid.add(lateFeeDue);

                scheduleRepository.updatePayment(schedule.getId(), principalDue, interestDue, lateFeeDue,
                        "PAID", LocalDate.now());

                log.info("INSTALLMENT_PAID")
                        .attr("scheduleId", schedule.getId())
                        .attr("principalPaid", principalDue)
                        .attr("interestPaid", interestDue)
                        .attr("lateFeePaid", lateFeeDue)
                        .log();
            } else {
                BigDecimal lateFeePayment = remainingAmount.min(lateFeeDue);
                remainingAmount = remainingAmount.subtract(lateFeePayment);
                totalLateFeePaid = totalLateFeePaid.add(lateFeePayment);

                BigDecimal interestPayment = remainingAmount.min(interestDue);
                remainingAmount = remainingAmount.subtract(interestPayment);
                totalInterestPaid = totalInterestPaid.add(interestPayment);

                BigDecimal principalPayment = remainingAmount.min(principalDue);
                remainingAmount = remainingAmount.subtract(principalPayment);
                totalPrincipalPaid = totalPrincipalPaid.add(principalPayment);

                scheduleRepository.updatePayment(schedule.getId(), principalPayment, interestPayment, lateFeePayment,
                        "PARTIALLY_PAID", null);

                log.info("INSTALLMENT_PARTIAL")
                        .attr("scheduleId", schedule.getId())
                        .attr("principalPaid", principalPayment)
                        .attr("interestPaid", interestPayment)
                        .attr("lateFeePaid", lateFeePayment)
                        .log();
                break;
            }
        }

        BigDecimal advancePayment = remainingAmount.max(BigDecimal.ZERO);
        BigDecimal totalAllocated = totalPrincipalPaid.add(totalInterestPaid).add(totalLateFeePaid);
        if (totalAllocated.compareTo(dto.getAmount()) > 0) {
            throw new BusinessRuleException("Over-allocation detected");
        }

        String message = null;
        if (advancePayment.compareTo(BigDecimal.ZERO) > 0) {
            boolean anyPending = scheduleRepository.findPendingByLoanId(dto.getLoanId()).size() > 0;
            message = anyPending
                    ? "Advance Payment Balance: ₹ " + advancePayment.setScale(2) + " will adjust next EMI."
                    : "Credit balance ₹ " + advancePayment.setScale(2) + ".";
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
            atroposEventPublisher.publishLoanClosedEvent(loanRepository.findById(dto.getLoanId()).get());
            log.info("REPAYMENT_LOAN_CLOSED").attr("loanId", dto.getLoanId()).log();
        } else if (updatedLoan.getStatus().name().equals("OVERDUE")) {
            boolean stillOverdue = scheduleRepository.findPendingByLoanId(dto.getLoanId()).stream()
                    .anyMatch(s -> LocalDate.now().isAfter(s.getDueDate().plusDays(loan.getGracePeriodDays())));
            if (!stillOverdue) {
                loanRepository.updateStatus(dto.getLoanId(), "ACTIVE");
                log.info("REPAYMENT_LOAN_STATUS_NORMALIZED").attr("loanId", dto.getLoanId()).log();
            }
        }

        atroposEventPublisher.publishLoanRepaymentEvent(repayment, loan);

        log.info("REPAYMENT_PROCESS_COMPLETE")
                .attr("repaymentId", repayment.getId())
                .attr("loanId", dto.getLoanId())
                .attr("principalPaidTotal", totalPrincipalPaid)
                .attr("interestPaidTotal", totalInterestPaid)
                .attr("lateFeePaidTotal", totalLateFeePaid)
                .attr("advanceRemaining", advancePayment)
                .attr("message", message)
                .log();

        return repaymentMapper.toResponse(repayment, message);
    }

    public List<RepaymentResponseDTO> getRepaymentsByLoan(UUID loanId) {
        return repaymentRepository.findByLoanId(loanId).stream()
                .map(r -> repaymentMapper.toResponse(r, null))
                .collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getRepaymentSchedule(UUID loanId) {
        return scheduleRepository.findByLoanId(loanId).stream()
                .map(scheduleMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RepaymentScheduleResponseDTO> getPendingSchedule(UUID loanId) {
        return scheduleRepository.findPendingByLoanId(loanId).stream()
                .map(scheduleMapper::toResponse)
                .collect(Collectors.toList());
    }

    private String generateReceiptNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%06d", new Random().nextInt(999999));
        return "RCP-" + date + "-" + rand;
    }
}