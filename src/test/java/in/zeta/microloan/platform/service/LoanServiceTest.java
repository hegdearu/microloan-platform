package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanProductRepository productRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private RepaymentScheduleService scheduleService;

    @Mock
    private AtroposEventPublisherService atroposEventPublisher;

    @Mock
    private LoanValidator loanValidator;

    @Mock
    private LoanMapper loanMapper;

    @InjectMocks
    private LoanService loanService;

    private LoanIssuanceRequestDTO issuanceDTO;
    private Loan loan;
    private LoanResponseDTO loanResponseDTO;
    private LoanApplication application;
    private LoanProduct product;
    private Borrower borrower;
    private Household household;
    private UUID loanId;
    private UUID borrowerId;
    private UUID productId;
    private UUID applicationId;
    private UUID householdId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        householdId = UUID.randomUUID();

        product = LoanProduct.builder()
                .id(productId)
                .status(LoanProductStatus.ACTIVE)
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .tenureMonths(24)
                .processingFeeType("PERCENTAGE")
                .processingFeeValue(new BigDecimal("2"))
                .gracePeriodDays(7)
                .lateFeePercent(new BigDecimal("1"))
                .build();

        borrower = Borrower.builder()
                .id(borrowerId)
                .name("Test Borrower")
                .phone("9876543210")
                .householdId(householdId)
                .isVerified(true)
                .status(UserStatus.ACTIVE)
                .build();

        household = Household.builder()
                .id(householdId)
                .totalAnnualIncome(new BigDecimal("500000"))
                .build();

        application = LoanApplication.builder()
                .id(applicationId)
                .borrowerId(borrowerId)
                .productId(productId)
                .status(LoanApplicationStatus.APPROVED)
                .approvedAmount(new BigDecimal("50000"))
                .build();

        issuanceDTO = LoanIssuanceRequestDTO.builder()
                .applicationId(applicationId)
                .borrowerId(borrowerId)
                .productId(productId)
                .principalAmount(new BigDecimal("50000"))
                .interestRate(new BigDecimal("12"))
                .tenureMonths(12)
                .repaymentFrequency("MONTHLY")
                .disbursementMethod("BANK_TRANSFER")
                .disbursementDate(LocalDate.now())
                .firstDueDate(LocalDate.now().plusMonths(1))
                .build();

        loan = Loan.builder()
                .id(loanId)
                .loanNumber("LN-2024-123456")
                .applicationId(applicationId)
                .borrowerId(borrowerId)
                .householdId(householdId)
                .productId(productId)
                .principalAmount(new BigDecimal("50000"))
                .interestRate(new BigDecimal("12"))
                .processingFee(new BigDecimal("1000"))
                .tenureMonths(12)
                .repaymentFrequency(RepaymentFrequency.MONTHLY)
                .emiAmount(new BigDecimal("4469"))
                .totalPayable(new BigDecimal("53628"))
                .outstandingPrincipal(new BigDecimal("50000"))
                .outstandingInterest(new BigDecimal("3628"))
                .totalOutstanding(new BigDecimal("53628"))
                .totalPaid(BigDecimal.ZERO)
                .disbursementDate(LocalDate.now())
                .disbursementMethod(DisbursementMethod.BANK_TRANSFER)
                .firstDueDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .build();

        loanResponseDTO = LoanResponseDTO.builder()
                .id(loanId)
                .loanNumber("LN-2024-123456")
                .borrowerId(borrowerId)
                .principalAmount(new BigDecimal("50000"))
                .emiAmount(new BigDecimal("4469"))
                .status(LoanStatus.ACTIVE)
                .build();
    }

    @Test
    void createLoan_WithValidData_ShouldSucceed() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        doNothing().when(loanValidator).validateCreate(any(), any(), any(), any());
        when(loanRepository.create(any(Loan.class))).thenReturn(loanId);
        doNothing().when(scheduleService).generateSchedule(any(), any(), any(), anyInt(), any(), any());
        doNothing().when(applicationRepository).updateStatus(any(), any());
        doNothing().when(atroposEventPublisher).publishLoanIssuedEvent(any());
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(loan)).thenReturn(loanResponseDTO);

        LoanResponseDTO result = loanService.createLoan(issuanceDTO, 1L);

        assertNotNull(result);
        assertEquals(loanId, result.getId());
        verify(loanRepository).create(any(Loan.class));
        verify(scheduleService).generateSchedule(any(), any(), any(), anyInt(), any(), any());
        verify(atroposEventPublisher).publishLoanIssuedEvent(any());
    }

    @Test
    void createLoan_WithoutApplication_ShouldSucceed() {
        issuanceDTO.setApplicationId(null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        doNothing().when(loanValidator).validateCreate(any(), any(), any(), any());
        when(loanRepository.create(any(Loan.class))).thenReturn(loanId);
        doNothing().when(scheduleService).generateSchedule(any(), any(), any(), anyInt(), any(), any());
        doNothing().when(atroposEventPublisher).publishLoanIssuedEvent(any());
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(loan)).thenReturn(loanResponseDTO);

        LoanResponseDTO result = loanService.createLoan(issuanceDTO, 1L);

        assertNotNull(result);
        verify(applicationRepository, never()).updateStatus(any(), any());
    }

    @Test
    void createLoan_WithApplicationNotFound_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                loanService.createLoan(issuanceDTO, 1L)
        );
    }

    @Test
    void createLoan_WithExistingLoanForApplication_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanRepository.existsByApplicationId(applicationId)).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                loanService.createLoan(issuanceDTO, 1L)
        );
    }

    @Test
    void createLoan_WithProductNotFound_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                loanService.createLoan(issuanceDTO, 1L)
        );
    }

    @Test
    void createLoan_WithBorrowerNotFound_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                loanService.createLoan(issuanceDTO, 1L)
        );
    }
}