package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.*;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final LoanProductRepository productRepository;
    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;
    private final LoanApplicationRepository applicationRepository;
    private final RepaymentScheduleService scheduleService;
    private final AtroposEventPublisherService atroposEventPublisher;

    public LoanService(LoanRepository loanRepository,
                       LoanProductRepository productRepository,
                       BorrowerRepository borrowerRepository,
                       HouseholdRepository householdRepository,
                       LoanApplicationRepository applicationRepository,
                       RepaymentScheduleService scheduleService,
                       AtroposEventPublisherService atroposEventPublisher) {
        this.loanRepository = loanRepository;
        this.productRepository = productRepository;
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
        this.applicationRepository = applicationRepository;
        this.scheduleService = scheduleService;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Transactional
    public LoanResponseDTO createLoan(LoanIssuanceRequestDTO dto, Long createdBy) {
        spectraLogger.info("LOAN_CREATE_ATTEMPT")
                .attr("borrowerId", dto.getBorrowerId())
                .attr("productId", dto.getProductId())
                .attr("principalAmount", dto.getPrincipalAmount())
                .attr("applicationId", dto.getApplicationId())
                .attr("createdBy", createdBy)
                .log();

        if (dto.getApplicationId() != null) {
            LoanApplication application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> {
                        spectraLogger.warn("LOAN_CREATE_APPLICATION_NOT_FOUND")
                                .attr("applicationId", dto.getApplicationId()).log();
                        return new ResourceNotFoundException("Loan application not found");
                    });

            if (application.getStatus() != LoanApplicationStatus.APPROVED) {
                spectraLogger.warn("LOAN_CREATE_APPLICATION_STATUS_INVALID")
                        .attr("applicationId", dto.getApplicationId())
                        .attr("status", application.getStatus().name())
                        .log();
                throw new BusinessRuleException("Loan can only be created for APPROVED applications. Current status: " +
                        application.getStatus());
            }

            if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
                spectraLogger.warn("LOAN_CREATE_APPLICATION_EXPIRED")
                        .attr("applicationId", dto.getApplicationId()).log();
                throw new BusinessRuleException("Loan application has expired");
            }

            if (loanRepository.existsByApplicationId(dto.getApplicationId())) {
                spectraLogger.warn("LOAN_CREATE_ALREADY_EXISTS_FOR_APPLICATION")
                        .attr("applicationId", dto.getApplicationId()).log();
                throw new BusinessRuleException("Loan already exists for this application");
            }

            if (application.getApprovedAmount() != null &&
                    dto.getPrincipalAmount().compareTo(application.getApprovedAmount()) != 0) {
                spectraLogger.warn("LOAN_CREATE_PRINCIPAL_MISMATCH_APPROVED")
                        .attr("applicationId", dto.getApplicationId())
                        .attr("principalAmount", dto.getPrincipalAmount())
                        .attr("approvedAmount", application.getApprovedAmount())
                        .log();
                throw new BusinessRuleException(String.format("Principal amount ₹%s does not match approved amount ₹%s",
                        dto.getPrincipalAmount(), application.getApprovedAmount()));
            }

            if (!application.getBorrowerId().equals(dto.getBorrowerId())) {
                spectraLogger.warn("LOAN_CREATE_BORROWER_MISMATCH_APPLICATION")
                        .attr("applicationBorrowerId", application.getBorrowerId())
                        .attr("requestBorrowerId", dto.getBorrowerId())
                        .log();
                throw new BusinessRuleException("Borrower ID does not match the loan application");
            }

            if (!application.getProductId().equals(dto.getProductId())) {
                spectraLogger.warn("LOAN_CREATE_PRODUCT_MISMATCH_APPLICATION")
                        .attr("applicationProductId", application.getProductId())
                        .attr("requestProductId", dto.getProductId())
                        .log();
                throw new BusinessRuleException("Product ID does not match the loan application");
            }
        }

        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_CREATE_PRODUCT_NOT_FOUND")
                            .attr("productId", dto.getProductId()).log();
                    return new ResourceNotFoundException("Loan product not found");
                });

        if (!"ACTIVE".equals(product.getStatus().name())) {
            spectraLogger.warn("LOAN_CREATE_PRODUCT_INACTIVE")
                    .attr("productId", dto.getProductId()).log();
            throw new BusinessRuleException("Cannot create loan for inactive product");
        }

        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_CREATE_BORROWER_NOT_FOUND")
                            .attr("borrowerId", dto.getBorrowerId()).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        if (!borrower.getIsVerified()) {
            spectraLogger.warn("LOAN_CREATE_BORROWER_NOT_VERIFIED")
                    .attr("borrowerId", dto.getBorrowerId()).log();
            throw new BusinessRuleException("Borrower must be verified before loan disbursement");
        }

        if (borrower.getStatus() != UserStatus.ACTIVE) {
            spectraLogger.warn("LOAN_CREATE_BORROWER_STATUS_INVALID")
                    .attr("borrowerId", dto.getBorrowerId())
                    .attr("status", borrower.getStatus().name())
                    .log();
            throw new BusinessRuleException("Borrower status must be ACTIVE. Current status: " +
                    borrower.getStatus());
        }

        if (dto.getPrincipalAmount().compareTo(product.getMinAmount()) < 0 ||
                dto.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            spectraLogger.warn("LOAN_CREATE_AMOUNT_OUT_OF_RANGE")
                    .attr("principalAmount", dto.getPrincipalAmount())
                    .attr("min", product.getMinAmount())
                    .attr("max", product.getMaxAmount())
                    .log();
            throw new BusinessRuleException(String.format("Loan amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }

        if (dto.getTenureMonths() > product.getTenureMonths()) {
            spectraLogger.warn("LOAN_CREATE_TENURE_EXCEEDED")
                    .attr("requestedTenure", dto.getTenureMonths())
                    .attr("maxTenure", product.getTenureMonths())
                    .log();
            throw new BusinessRuleException(String.format("Tenure cannot exceed %d months for this product",
                    product.getTenureMonths()));
        }

        BigDecimal emiAmount = EMICalculator.calculateEMI(
                dto.getPrincipalAmount(), dto.getInterestRate(), dto.getTenureMonths());

        BigDecimal processingFee = calculateProcessingFee(dto.getPrincipalAmount(), product);
        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(emiAmount, dto.getTenureMonths(), dto.getPrincipalAmount());
        BigDecimal totalPayable = dto.getPrincipalAmount().add(totalInterest);

        BigDecimal householdIncome = null;
        if (borrower.getHouseholdId() != null) {
            Household household = householdRepository.findById(borrower.getHouseholdId()).orElse(null);
            if (household != null) {
                householdIncome = household.getTotalAnnualIncome();
            }
        }

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .applicationId(dto.getApplicationId())
                .borrowerId(dto.getBorrowerId())
                .householdId(borrower.getHouseholdId())
                .productId(dto.getProductId())
                .principalAmount(dto.getPrincipalAmount())
                .interestRate(dto.getInterestRate())
                .processingFee(processingFee)
                .tenureMonths(dto.getTenureMonths())
                .repaymentFrequency(RepaymentFrequency.valueOf(dto.getRepaymentFrequency()))
                .emiAmount(emiAmount)
                .totalPayable(totalPayable)
                .outstandingPrincipal(dto.getPrincipalAmount())
                .outstandingInterest(totalInterest)
                .totalOutstanding(totalPayable)
                .totalPaid(BigDecimal.ZERO)
                .disbursementDate(dto.getDisbursementDate())
                .disbursementMethod(DisbursementMethod.valueOf(dto.getDisbursementMethod()))
                .firstDueDate(dto.getFirstDueDate())
                .status(LoanStatus.ACTIVE)
                .gracePeriodDays(product.getGracePeriodDays())
                .lateFeePercent(product.getLateFeePercent())
                .householdIncomeAtApproval(householdIncome)
                .createdBy(createdBy)
                .build();

        UUID loanId = loanRepository.create(loan);
        loan.setId(loanId);

        scheduleService.generateSchedule(loanId, dto.getPrincipalAmount(), dto.getInterestRate(),
                dto.getTenureMonths(), emiAmount, dto.getFirstDueDate());

        if (dto.getApplicationId() != null) {
            applicationRepository.updateStatus(dto.getApplicationId(), LoanApplicationStatus.DISBURSED);
        }

        atroposEventPublisher.publishLoanIssuedEvent(loan);

        LoanResponseDTO response = loanRepository.findById(loanId)
                .map(this::mapToResponseDTO)
                .orElse(mapToResponseDTO(loan));

        spectraLogger.info("LOAN_CREATE_SUCCESS")
                .attr("loanId", response.getId())
                .attr("loanNumber", response.getLoanNumber())
                .attr("emiAmount", response.getEmiAmount())
                .log();
        return response;
    }

    public LoanResponseDTO getLoanById(UUID id) {
        spectraLogger.info("LOAN_FETCH_BY_ID_ATTEMPT").attr("loanId", id).log();
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_FETCH_BY_ID_NOT_FOUND").attr("loanId", id).log();
                    return new ResourceNotFoundException("Loan not found");
                });
        spectraLogger.info("LOAN_FETCH_BY_ID_SUCCESS").attr("loanId", id).log();
        return mapToResponseDTO(loan);
    }

    public LoanDetailResponseDTO getLoanDetails(UUID id) {
        spectraLogger.info("LOAN_DETAILS_FETCH_ATTEMPT").attr("loanId", id).log();
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_DETAILS_FETCH_NOT_FOUND").attr("loanId", id).log();
                    return new ResourceNotFoundException("Loan not found");
                });

        Borrower borrower = borrowerRepository.findById(loan.getBorrowerId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_DETAILS_FETCH_BORROWER_NOT_FOUND")
                            .attr("borrowerId", loan.getBorrowerId()).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        spectraLogger.info("LOAN_DETAILS_FETCH_SUCCESS").attr("loanId", id).log();

        return LoanDetailResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .borrowerId(borrower.getId())
                .borrowerName(borrower.getName())
                .borrowerPhone(borrower.getPhone())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .totalPayable(loan.getTotalPayable())
                .outstandingPrincipal(loan.getOutstandingPrincipal())
                .outstandingInterest(loan.getOutstandingInterest())
                .totalOutstanding(loan.getTotalOutstanding())
                .totalPaid(loan.getTotalPaid())
                .disbursementDate(loan.getDisbursementDate())
                .firstDueDate(loan.getFirstDueDate())
                .lastPaymentDate(loan.getLastPaymentDate())
                .status(loan.getStatus())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    public List<LoanResponseDTO> getLoansByBorrower(UUID borrowerId) {
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOANS_BY_BORROWER_BORROWER_NOT_FOUND")
                            .attr("borrowerId", borrowerId).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        List<LoanResponseDTO> result = loans.stream().map(this::mapToResponseDTO).collect(Collectors.toList());

        spectraLogger.info("LOANS_BY_BORROWER_SUCCESS")
                .attr("borrowerId", borrowerId)
                .attr("count", result.size())
                .log();
        return result;
    }

    public List<LoanResponseDTO> getLoansByHousehold(UUID householdId) {
        householdRepository.findById(householdId)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOANS_BY_HOUSEHOLD_NOT_FOUND")
                            .attr("householdId", householdId).log();
                    return new ResourceNotFoundException("Household not found");
                });

        List<Loan> loans = loanRepository.findByHouseholdId(householdId);
        List<LoanResponseDTO> result = loans.stream().map(this::mapToResponseDTO).collect(Collectors.toList());

        spectraLogger.info("LOANS_BY_HOUSEHOLD_SUCCESS")
                .attr("householdId", householdId)
                .attr("count", result.size())
                .log();
        return result;
    }

    public List<LoanResponseDTO> getLoansByStatus(String status, int page, int limit) {
        spectraLogger.info("LOANS_BY_STATUS_ATTEMPT")
                .attr("status", status)
                .attr("page", page)
                .attr("limit", limit)
                .log();
        try {
            LoanStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            spectraLogger.warn("LOANS_BY_STATUS_INVALID")
                    .attr("status", status)
                    .log();
            throw new BusinessRuleException("Invalid loan status: " + status);
        }

        List<Loan> loans = loanRepository.findByStatus(status.toUpperCase());
        List<LoanResponseDTO> result = loans.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        spectraLogger.info("LOANS_BY_STATUS_SUCCESS")
                .attr("status", status)
                .attr("count", result.size())
                .log();
        return result;
    }

    @Transactional
    public void cancelLoan(UUID id, String reason) {
        spectraLogger.info("LOAN_CANCEL_ATTEMPT")
                .attr("loanId", id)
                .attr("reason", reason)
                .log();

        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_CANCEL_NOT_FOUND").attr("loanId", id).log();
                    return new ResourceNotFoundException("Loan not found");
                });

        if (!"ACTIVE".equals(loan.getStatus().name()) && !"DISBURSED".equals(loan.getStatus().name())) {
            spectraLogger.warn("LOAN_CANCEL_STATUS_INVALID")
                    .attr("loanId", id)
                    .attr("status", loan.getStatus().name())
                    .log();
            throw new BusinessRuleException("Only active or disbursed loans can be cancelled");
        }

        if (loan.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
            spectraLogger.warn("LOAN_CANCEL_HAS_PAYMENTS")
                    .attr("loanId", id)
                    .attr("totalPaid", loan.getTotalPaid())
                    .log();
            throw new BusinessRuleException("Cannot cancel loan with payments already made");
        }

        loanRepository.updateStatus(id, "CANCELLED");
        atroposEventPublisher.publishLoanCancelledEvent(loan, reason);

        spectraLogger.info("LOAN_CANCEL_SUCCESS")
                .attr("loanId", id)
                .log();
    }

    private BigDecimal calculateProcessingFee(BigDecimal principalAmount, LoanProduct product) {
        if ("PERCENTAGE".equals(product.getProcessingFeeType())) {
            return principalAmount.multiply(product.getProcessingFeeValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            return product.getProcessingFeeValue();
        }
    }

    private String generateLoanNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "LN-" + year + "-" + randomPart;
    }

    private LoanResponseDTO mapToResponseDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .borrowerId(loan.getBorrowerId())
                .householdId(loan.getHouseholdId())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .totalPayable(loan.getTotalPayable())
                .totalOutstanding(loan.getTotalOutstanding())
                .totalPaid(loan.getTotalPaid())
                .disbursementDate(loan.getDisbursementDate())
                .firstDueDate(loan.getFirstDueDate())
                .status(loan.getStatus())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}