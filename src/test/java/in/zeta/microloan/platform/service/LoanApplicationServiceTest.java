package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.microloan.platform.service.mappers.LoanApplicationMapper;
import in.zeta.microloan.platform.service.validator.LoanApplicationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private LoanProductRepository productRepository;

    @Mock
    private AtroposEventPublisherService atroposEventPublisher;

    @Mock
    private LoanApplicationValidator validator;

    @Mock
    private LoanApplicationMapper mapper;

    @InjectMocks
    private LoanApplicationService applicationService;

    private LoanApplicationRequestDTO requestDTO;
    private LoanApplication application;
    private LoanApplicationResponseDTO responseDTO;
    private Borrower borrower;
    private LoanProduct product;
    private UUID borrowerId;
    private UUID productId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        borrowerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        ReflectionTestUtils.setField(applicationService, "maxActiveLoans", 3);
        ReflectionTestUtils.setField(applicationService, "applicationExpiryDays", 7);

        borrower = Borrower.builder()
                .id(borrowerId)
                .name("Test Borrower")
                .isVerified(true)
                .build();

        product = LoanProduct.builder()
                .id(productId)
                .name("Personal Loan")
                .status(LoanProductStatus.ACTIVE)
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .tenureMonths(24)
                .build();

        requestDTO = LoanApplicationRequestDTO.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("50000"))
                .purpose("Business")
                .preferredTenure(12)
                .build();

        application = LoanApplication.builder()
                .id(applicationId)
                .applicationNumber("APP-20240101-123456")
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("50000"))
                .purpose("Business")
                .preferredTenure(12)
                .status(LoanApplicationStatus.PENDING_REVIEW)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        responseDTO = LoanApplicationResponseDTO.builder()
                .id(applicationId)
                .applicationNumber("APP-20240101-123456")
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("50000"))
                .status("PENDING_REVIEW")
                .build();
    }

    @Test
    void createApplication_WithValidData_ShouldSucceed() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(0);
        when(applicationRepository.hasPendingApplication(borrowerId)).thenReturn(false);
        doNothing().when(validator).validateCreate(any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        when(applicationRepository.create(any(LoanApplication.class))).thenReturn(application);
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        LoanApplicationResponseDTO result = applicationService.createApplication(requestDTO);

        assertNotNull(result);
        assertEquals(applicationId, result.getId());
        verify(applicationRepository).create(any(LoanApplication.class));
    }

    @Test
    void createApplication_WithNonExistentBorrower_ShouldThrowException() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.createApplication(requestDTO)
        );
    }

    @Test
    void createApplication_WithNonExistentProduct_ShouldThrowException() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.createApplication(requestDTO)
        );
    }

    @Test
    void createApplication_WithValidationFailure_ShouldThrowException() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(0);
        when(applicationRepository.hasPendingApplication(borrowerId)).thenReturn(false);
        doThrow(new BusinessRuleException("Validation failed"))
                .when(validator).validateCreate(any(), any(), any(), anyInt(), anyInt(), anyBoolean());

        assertThrows(BusinessRuleException.class, () ->
                applicationService.createApplication(requestDTO)
        );
    }

    @Test
    void createApplication_WithNullPreferredTenure_ShouldUseProductTenure() {
        requestDTO.setPreferredTenure(null);
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(0);
        when(applicationRepository.hasPendingApplication(borrowerId)).thenReturn(false);
        doNothing().when(validator).validateCreate(any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        when(applicationRepository.create(any(LoanApplication.class))).thenReturn(application);
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        LoanApplicationResponseDTO result = applicationService.createApplication(requestDTO);

        assertNotNull(result);
    }

    @Test
    void getApplicationsByBorrower_WhenBorrowerExists_ShouldReturnList() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(applicationRepository.findByBorrowerId(borrowerId)).thenReturn(Arrays.asList(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        List<LoanApplicationResponseDTO> result = applicationService.getApplicationsByBorrower(borrowerId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getApplicationsByBorrower_WhenBorrowerNotExists_ShouldThrowException() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.getApplicationsByBorrower(borrowerId)
        );
    }

    @Test
    void getApplicationsByStatus_WithValidStatus_ShouldReturnList() {
        when(applicationRepository.findByStatus(LoanApplicationStatus.PENDING_REVIEW))
                .thenReturn(Arrays.asList(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        List<LoanApplicationResponseDTO> result = applicationService.getApplicationsByStatus("PENDING_REVIEW", 1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getApplicationsByStatus_WithInvalidStatus_ShouldThrowException() {
        assertThrows(ValidationException.class, () ->
                applicationService.getApplicationsByStatus("INVALID_STATUS", 1, 20)
        );
    }

    @Test
    void getAllApplications_ShouldReturnPaginatedList() {
        when(applicationRepository.findAll()).thenReturn(Arrays.asList(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        List<LoanApplicationResponseDTO> result = applicationService.getAllApplications(1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getApplicationById_WhenExists_ShouldReturn() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        LoanApplicationResponseDTO result = applicationService.getApplicationById(applicationId);

        assertNotNull(result);
        assertEquals(applicationId, result.getId());
    }

    @Test
    void getApplicationById_WhenNotExists_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.getApplicationById(applicationId)
        );
    }

    @Test
    void getPendingApplications_ShouldReturnList() {
        when(applicationRepository.findPendingApplications()).thenReturn(Arrays.asList(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        List<LoanApplicationResponseDTO> result = applicationService.getPendingApplications(1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getExpiredApplications_ShouldReturnList() {
        when(applicationRepository.findExpiredApplications()).thenReturn(Arrays.asList(application));
        when(mapper.toResponse(application)).thenReturn(responseDTO);

        List<LoanApplicationResponseDTO> result = applicationService.getExpiredApplications(1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void approveApplication_WithValidData_ShouldSucceed() {
        BigDecimal approvedAmount = new BigDecimal("45000");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(validator).validateApproveAmount(approvedAmount, product);
        doNothing().when(applicationRepository).approve(applicationId, approvedAmount);
        doNothing().when(atroposEventPublisher).publishApplicationApprovedEvent(any());
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        LoanApplicationResponseDTO result = applicationService.approveApplication(applicationId, approvedAmount);

        assertNotNull(result);
        verify(applicationRepository).approve(applicationId, approvedAmount);
        verify(atroposEventPublisher).publishApplicationApprovedEvent(any());
    }

    @Test
    void approveApplication_WhenNotExists_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("45000");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.approveApplication(applicationId, approvedAmount)
        );
    }

    @Test
    void approveApplication_WithNonPendingStatus_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("45000");
        application.setStatus(LoanApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BusinessRuleException.class, () ->
                applicationService.approveApplication(applicationId, approvedAmount)
        );
    }

    @Test
    void approveApplication_WithExpiredApplication_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("45000");
        application.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BusinessRuleException.class, () ->
                applicationService.approveApplication(applicationId, approvedAmount)
        );
    }

    @Test
    void approveApplication_WithProductNotFound_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("45000");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.approveApplication(applicationId, approvedAmount)
        );
    }

    @Test
    void approveApplication_WithInvalidAmount_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("150000");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doThrow(new ValidationException("Amount out of range"))
                .when(validator).validateApproveAmount(approvedAmount, product);

        assertThrows(ValidationException.class, () ->
                applicationService.approveApplication(applicationId, approvedAmount)
        );
    }

    @Test
    void rejectApplication_WithValidData_ShouldSucceed() {
        String rejectionReason = "Insufficient documentation";
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        doNothing().when(applicationRepository).reject(applicationId, rejectionReason);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        doNothing().when(atroposEventPublisher).publishApplicationRejectedEvent(any(), anyString());

        applicationService.rejectApplication(applicationId, rejectionReason);

        verify(applicationRepository).reject(applicationId, rejectionReason);
        verify(atroposEventPublisher).publishApplicationRejectedEvent(any(), eq(rejectionReason));
    }

    @Test
    void rejectApplication_WhenNotExists_ShouldThrowException() {
        String rejectionReason = "Insufficient documentation";
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.rejectApplication(applicationId, rejectionReason)
        );
    }

    @Test
    void rejectApplication_WithNonPendingStatus_ShouldThrowException() {
        String rejectionReason = "Insufficient documentation";
        application.setStatus(LoanApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BusinessRuleException.class, () ->
                applicationService.rejectApplication(applicationId, rejectionReason)
        );
    }

    @Test
    void rejectApplication_WithEmptyReason_ShouldThrowException() {
        String rejectionReason = "";
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ValidationException.class, () ->
                applicationService.rejectApplication(applicationId, rejectionReason)
        );
    }

    @Test
    void rejectApplication_WithNullReason_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ValidationException.class, () ->
                applicationService.rejectApplication(applicationId, null)
        );
    }

    @Test
    void cancelApplication_WithValidData_ShouldSucceed() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        doNothing().when(applicationRepository).cancel(applicationId);

        applicationService.cancelApplication(applicationId);

        verify(applicationRepository).cancel(applicationId);
    }

    @Test
    void cancelApplication_WhenNotExists_ShouldThrowException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.cancelApplication(applicationId)
        );
    }

    @Test
    void cancelApplication_WithApprovedStatus_ShouldThrowException() {
        application.setStatus(LoanApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BusinessRuleException.class, () ->
                applicationService.cancelApplication(applicationId)
        );
    }

    @Test
    void cancelApplication_WithDisbursedStatus_ShouldThrowException() {
        application.setStatus(LoanApplicationStatus.DISBURSED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BusinessRuleException.class, () ->
                applicationService.cancelApplication(applicationId)
        );
    }
}