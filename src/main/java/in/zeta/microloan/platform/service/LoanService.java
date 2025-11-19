package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.*;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.microloan.platform.service.mappers.LoanMapper;
import in.zeta.microloan.platform.service.validator.LoanValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static in.zeta.microloan.platform.constants.LogConstants.*;
import static in.zeta.microloan.platform.constants.LogConstants.COUNT;
import static in.zeta.microloan.platform.exception.Error.*;
import static in.zeta.microloan.platform.exception.Error.LOAN_NOT_FOUND;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanProductRepository productRepository;
    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;
    private final LoanApplicationRepository applicationRepository;
    private final RepaymentScheduleService scheduleService;
    private final AtroposEventPublisherService atroposEventPublisher;
    private final LoanValidator loanValidator;
    private final LoanMapper loanMapper;

    public LoanService(LoanRepository loanRepository,
                       LoanProductRepository productRepository,
                       BorrowerRepository borrowerRepository,
                       HouseholdRepository householdRepository,
                       LoanApplicationRepository applicationRepository,
                       RepaymentScheduleService scheduleService,
                       AtroposEventPublisherService atroposEventPublisher,
                       LoanValidator loanValidator,
                       LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.productRepository = productRepository;
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
        this.applicationRepository = applicationRepository;
        this.scheduleService = scheduleService;
        this.atroposEventPublisher = atroposEventPublisher;
        this.loanValidator = loanValidator;
        this.loanMapper = loanMapper;
    }

    @Transactional
    public LoanResponseDTO createLoan(LoanIssuanceRequestDTO dto, Long createdBy) {

        LoanApplication application = null;
        if (dto.getApplicationId() != null) {
            application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND));
            if (loanRepository.existsByApplicationId(dto.getApplicationId())) {
                throw new RuntimeException("Loan already exists for this application");
            }
        }

        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND));
        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException(BORROWER_NOT_FOUND));

        loanValidator.validateCreate(dto, application, product, borrower);

        BigDecimal emiAmount = EMICalculator.calculateEMI(
                dto.getPrincipalAmount(), dto.getInterestRate(), dto.getTenureMonths());
        BigDecimal processingFee = calculateProcessingFee(dto.getPrincipalAmount(), product);
        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(emiAmount, dto.getTenureMonths(), dto.getPrincipalAmount());
        BigDecimal totalPayable = dto.getPrincipalAmount().add(totalInterest);

        BigDecimal householdIncome = null;
        if (borrower.getHouseholdId() != null) {
            Household h = householdRepository.findById(borrower.getHouseholdId()).orElse(null);
            if (h != null) householdIncome = h.getTotalAnnualIncome();
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

        if (application != null) {
            applicationRepository.updateStatus(application.getId(), LoanApplicationStatus.DISBURSED);
        }

        atroposEventPublisher.publishLoanIssuedEvent(loan);

        LoanResponseDTO response = loanRepository.findById(loanId)
                .map(loanMapper::toResponse)
                .orElse(loanMapper.toResponse(loan));

        return response;
    }

    public LoanResponseDTO getLoanById(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_NOT_FOUND));
        return loanMapper.toResponse(loan);
    }

    public LoanDetailResponseDTO getLoanDetails(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_NOT_FOUND));
        Borrower borrower = borrowerRepository.findById(loan.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException(BORROWER_NOT_FOUND));
        return loanMapper.toDetail(loan, borrower);
    }

    public List<LoanResponseDTO> getLoansByBorrower(UUID borrowerId) {
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException(BORROWER_NOT_FOUND));
        List<LoanResponseDTO> result = loanRepository.findByBorrowerId(borrowerId).stream()
                .map(loanMapper::toResponse).toList();
        return result;
    }

    public List<LoanResponseDTO> getLoansByHousehold(UUID householdId) {
        householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException(HOUSEHOLD_NOT_FOUND));
        List<LoanResponseDTO> result = loanRepository.findByHouseholdId(householdId).stream()
                .map(loanMapper::toResponse).toList();
        return result;
    }

    public List<LoanResponseDTO> getLoansByStatus(String status, int page, int limit) {
        LoanStatus.valueOf(status.toUpperCase());
        List<LoanResponseDTO> result = loanRepository.findByStatus(status.toUpperCase()).stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(loanMapper::toResponse)
                .toList();
        return result;
    }

    @Transactional
    public void cancelLoan(UUID id, String reason) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_NOT_FOUND));
        if (!(loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.DISBURSED)) {
            throw new RuntimeException("Only active or disbursed loans can be cancelled");
        }
        if (loan.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot cancel loan with payments already made");
        }
        loanRepository.updateStatus(id, "CANCELLED");
        atroposEventPublisher.publishLoanCancelledEvent(loan, reason);
    }

    private BigDecimal calculateProcessingFee(BigDecimal principalAmount, LoanProduct product) {
        if ("PERCENTAGE".equals(product.getProcessingFeeType())) {
            return principalAmount.multiply(product.getProcessingFeeValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return product.getProcessingFeeValue();
    }

    private String generateLoanNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String rand = String.format("%06d", new Random().nextInt(999999));
        return "LN-" + year + "-" + rand;
    }
}