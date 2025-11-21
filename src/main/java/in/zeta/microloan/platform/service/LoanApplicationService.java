package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.microloan.platform.service.mappers.LoanApplicationMapper;
import in.zeta.microloan.platform.service.validator.LoanApplicationValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static in.zeta.microloan.platform.exception.Error.*;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanProductRepository productRepository;
    private final AtroposEventPublisherService atroposEventPublisher;
    private final LoanApplicationValidator validator;
    private final LoanApplicationMapper mapper;

    @Value("${app.max-active-loans-per-borrower:3}")
    private int maxActiveLoans;

    @Value("${app.application-expiry-days:7}")
    private int applicationExpiryDays;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  BorrowerRepository borrowerRepository,
                                  LoanProductRepository productRepository,
                                  AtroposEventPublisherService atroposEventPublisher,
                                  LoanApplicationValidator validator,
                                  LoanApplicationMapper mapper) {
        this.applicationRepository = applicationRepository;
        this.borrowerRepository = borrowerRepository;
        this.productRepository = productRepository;
        this.atroposEventPublisher = atroposEventPublisher;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public LoanApplicationResponseDTO createApplication(LoanApplicationRequestDTO dto) {

        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(BORROWER_NOT_FOUND);
                });

        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND);
                });

        int activeLoansCount = borrowerRepository.countActiveLoansByBorrower(dto.getBorrowerId());
        boolean hasPendingApplication = applicationRepository.hasPendingApplication(dto.getBorrowerId());

        // Validate using validator component
        try {
            validator.validateCreate(dto, borrower, product, activeLoansCount, maxActiveLoans, hasPendingApplication);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (ValidationException e) {
            throw e;
        }

        LoanApplication application = LoanApplication.builder()
                .applicationNumber(generateApplicationNumber())
                .borrowerId(dto.getBorrowerId())
                .productId(dto.getProductId())
                .requestedAmount(dto.getRequestedAmount())
                .purpose(dto.getPurpose())
                .preferredTenure(dto.getPreferredTenure() != null ? dto.getPreferredTenure() : product.getTenureMonths())
                .status(LoanApplicationStatus.PENDING_REVIEW)
                .expiresAt(LocalDateTime.now().plusDays(applicationExpiryDays))
                .build();

        LoanApplication storedApplication = applicationRepository.create(application);

        return mapper.toResponse(storedApplication);
    }

    public List<LoanApplicationResponseDTO> getApplicationsByBorrower(UUID borrowerId) {
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(BORROWER_NOT_FOUND);
                });

        List<LoanApplication> applications = applicationRepository.findByBorrowerId(borrowerId);
        List<LoanApplicationResponseDTO> result = applications.stream()
                .map(mapper::toResponse)
                .toList();

        return result;
    }

    public List<LoanApplicationResponseDTO> getApplicationsByStatus(String status, int page, int limit) {
        try {
            LoanApplicationStatus applicationStatus = LoanApplicationStatus.valueOf(status.toUpperCase());
            List<LoanApplication> applications = applicationRepository.findByStatus(applicationStatus);
            List<LoanApplicationResponseDTO> result = applications.stream()
                    .skip( (long)(page - 1) * limit)
                    .limit(limit)
                    .map(mapper::toResponse)
                    .toList();
            return result;
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid application status: " + status);
        }
    }

    public List<LoanApplicationResponseDTO> getAllApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findAll();
        List<LoanApplicationResponseDTO> result = applications.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();
        return result;
    }

    public LoanApplicationResponseDTO getApplicationById(UUID id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });
        return mapper.toResponse(application);
    }

    public List<LoanApplicationResponseDTO> getPendingApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findPendingApplications();
        List<LoanApplicationResponseDTO> result = applications.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();
        return result;
    }

    public List<LoanApplicationResponseDTO> getExpiredApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findExpiredApplications();
        List<LoanApplicationResponseDTO> result = applications.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();
        return result;
    }

    @Transactional
    public LoanApplicationResponseDTO approveApplication(UUID id, BigDecimal approvedAmount) {

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("Only pending or under-verification applications can be approved");
        }

        if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
            throw new BusinessRuleException("Application has expired. Please submit a new application");
        }

        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND);
                });

        // Validate using validator component
        try {
            validator.validateApproveAmount(approvedAmount, product, application);
        } catch (ValidationException e) {
            throw e;
        }

        applicationRepository.approve(id, approvedAmount);
        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setApprovedAmount(approvedAmount);
        application.setApprovedAt(LocalDateTime.now());

        atroposEventPublisher.publishApplicationApprovedEvent(application);

        return mapper.toResponse(application);
    }

    @Transactional
    public void rejectApplication(UUID id, String rejectionReason) {

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("Only pending or under-verification applications can be rejected");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new ValidationException("Rejection reason is required");
        }

        applicationRepository.reject(id, rejectionReason);
        application = applicationRepository.findById(id).get();
        atroposEventPublisher.publishApplicationRejectedEvent(application, rejectionReason);
    }

    @Transactional
    public void cancelApplication(UUID id) {

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() == LoanApplicationStatus.APPROVED ||
                application.getStatus() == LoanApplicationStatus.DISBURSED) {
            throw new BusinessRuleException("Approved or disbursed applications cannot be cancelled");
        }

        applicationRepository.cancel(id);

    }

    private String generateApplicationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "APP-" + datePart + "-" + randomPart;
    }
}