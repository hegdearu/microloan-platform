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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class LoanService {

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
                       RepaymentScheduleService scheduleService, AtroposEventPublisherService atroposEventPublisher) {
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
        // Validate loan application if provided
        if (dto.getApplicationId() != null) {
            LoanApplication application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

            // Check if application is approved
            if (application.getStatus() != LoanApplicationStatus.APPROVED) {
                throw new BusinessRuleException(
                        "Loan can only be created for APPROVED applications. Current status: " +
                                application.getStatus());
            }

            // Check if application has expired
            if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
                throw new BusinessRuleException("Loan application has expired");
            }

            // Check if loan already exists for this application
            if (loanRepository.existsByApplicationId(dto.getApplicationId())) {
                throw new BusinessRuleException("Loan already exists for this application");
            }

            // Validate that the principal amount matches approved amount
            if (application.getApprovedAmount() != null &&
                    dto.getPrincipalAmount().compareTo(application.getApprovedAmount()) != 0) {
                throw new BusinessRuleException(
                        String.format("Principal amount ₹%s does not match approved amount ₹%s",
                                dto.getPrincipalAmount(), application.getApprovedAmount()));
            }

            // Validate borrower matches
            if (!application.getBorrowerId().equals(dto.getBorrowerId())) {
                throw new BusinessRuleException("Borrower ID does not match the loan application");
            }

            // Validate product matches
            if (!application.getProductId().equals(dto.getProductId())) {
                throw new BusinessRuleException("Product ID does not match the loan application");
            }
        }

        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        // Validate product is active
        if (!"ACTIVE".equals(product.getStatus().name())) {
            throw new BusinessRuleException("Cannot create loan for inactive product");
        }

        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        // Validate borrower is verified
        if (!borrower.getIsVerified()) {
            throw new BusinessRuleException("Borrower must be verified before loan disbursement");
        }

        // Validate borrower is active
        if (borrower.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Borrower status must be ACTIVE. Current status: " +
                    borrower.getStatus());
        }

        // Validate amount is within product limits
        if (dto.getPrincipalAmount().compareTo(product.getMinAmount()) < 0 ||
                dto.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessRuleException(
                    String.format("Loan amount must be between ₹%s and ₹%s",
                            product.getMinAmount(), product.getMaxAmount()));
        }

        // Validate tenure
        if (dto.getTenureMonths() > product.getTenureMonths()) {
            throw new BusinessRuleException(
                    String.format("Tenure cannot exceed %d months for this product",
                            product.getTenureMonths()));
        }

        BigDecimal emiAmount = EMICalculator.calculateEMI(
                dto.getPrincipalAmount(),
                dto.getInterestRate(),
                dto.getTenureMonths()
        );

        BigDecimal processingFee = calculateProcessingFee(dto.getPrincipalAmount(), product);

        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(
                emiAmount,
                dto.getTenureMonths(),
                dto.getPrincipalAmount()
        );

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

        Long loanId = loanRepository.create(loan);
        loan.setId(loanId);

        // Generate repayment schedule
        scheduleService.generateSchedule(loanId, dto.getPrincipalAmount(), dto.getInterestRate(),
                dto.getTenureMonths(), emiAmount, dto.getFirstDueDate());

        // Update application status to DISBURSED if application exists
        if (dto.getApplicationId() != null) {
            applicationRepository.updateStatus(dto.getApplicationId(), LoanApplicationStatus.DISBURSED);
        }

        // Publish event
        atroposEventPublisher.publishLoanIssuedEvent(loan);

        // Fetch the loan again to get the timestamps
        return loanRepository.findById(loanId)
                .map(this::mapToResponseDTO)
                .orElse(mapToResponseDTO(loan));
    }

    public LoanResponseDTO getLoanById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        return mapToResponseDTO(loan);
    }

    public LoanDetailResponseDTO getLoanDetails(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        Borrower borrower = borrowerRepository.findById(loan.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

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

    public List<LoanResponseDTO> getLoansByBorrower(Long borrowerId) {
        // Validate borrower exists
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        return loans.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanResponseDTO> getLoansByHousehold(Long householdId) {
        // Validate household exists
        householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));

        List<Loan> loans = loanRepository.findByHouseholdId(householdId);
        return loans.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanResponseDTO> getLoansByStatus(String status, int page, int limit) {
        try {
            LoanStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid loan status: " + status);
        }

        List<Loan> loans = loanRepository.findByStatus(status.toUpperCase());
        return loans.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelLoan(Long id, String reason) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!"ACTIVE".equals(loan.getStatus().name()) && !"DISBURSED".equals(loan.getStatus().name())) {
            throw new BusinessRuleException("Only active or disbursed loans can be cancelled");
        }

        // Check if any payments have been made
        if (loan.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot cancel loan with payments already made");
        }

        loanRepository.updateStatus(id, "CANCELLED");

        atroposEventPublisher.publishLoanCancelledEvent(loan, reason);
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