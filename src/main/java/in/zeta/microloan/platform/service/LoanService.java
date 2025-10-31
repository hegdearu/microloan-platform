package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.LoanIssuanceDTO;
import in.zeta.microloan.platform.dto.LoanResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
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
    private final RepaymentScheduleService scheduleService;
    private final EventPublisherService eventPublisher;

    public LoanService(LoanRepository loanRepository,
                       LoanProductRepository productRepository,
                       BorrowerRepository borrowerRepository,
                       HouseholdRepository householdRepository,
                       RepaymentScheduleService scheduleService,
                       EventPublisherService eventPublisher) {
        this.loanRepository = loanRepository;
        this.productRepository = productRepository;
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
        this.scheduleService = scheduleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LoanResponseDTO createLoan(LoanIssuanceDTO dto, Long createdBy) {
        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

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

        scheduleService.generateSchedule(loanId, dto.getPrincipalAmount(), dto.getInterestRate(),
                dto.getTenureMonths(), emiAmount, dto.getFirstDueDate());

        eventPublisher.publishLoanDisbursedEvent(loan);

        return mapToResponseDTO(loan);
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
        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        return loans.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanResponseDTO> getLoansByHousehold(Long householdId) {
        List<Loan> loans = loanRepository.findByHouseholdId(householdId);
        return loans.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanResponseDTO> getLoansByStatus(String status, int page, int limit) {
        List<Loan> loans = loanRepository.findByStatus(status);
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

        if (!"ACTIVE".equals(loan.getStatus()) && !"DISBURSED".equals(loan.getStatus())) {
            throw new BusinessRuleException("Only active or disbursed loans can be cancelled");
        }

        loanRepository.updateStatus(id, "CANCELLED");

        eventPublisher.publishLoanCancelledEvent(loan, reason);
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
