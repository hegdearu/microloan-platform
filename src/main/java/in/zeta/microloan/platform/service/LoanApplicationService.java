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
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.zeta.microloan.platform.constants.LogConstants.*;
import static in.zeta.microloan.platform.constants.LogConstants.APPROVED_AMOUNT;
import static in.zeta.microloan.platform.exception.Error.*;

@Service
public class LoanApplicationService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanApplicationService.class);

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
        spectraLogger.info("LOAN_APPLICATION_CREATE_ATTEMPT")
                .attr(BORROWER_ID, dto.getBorrowerId())
                .attr(PRODUCT_ID, dto.getProductId())
                .attr("requestedAmount", dto.getRequestedAmount())
                .log();

        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_CREATE_BORROWER_NOT_FOUND")
                            .attr(BORROWER_ID, dto.getBorrowerId())
                            .log();
                    return new ResourceNotFoundException(BORROWER_NOT_FOUND);
                });

        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_CREATE_PRODUCT_NOT_FOUND")
                            .attr(PRODUCT_ID, dto.getProductId())
                            .log();
                    return new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND);
                });

        int activeLoansCount = borrowerRepository.countActiveLoansByBorrower(dto.getBorrowerId());
        boolean hasPendingApplication = applicationRepository.hasPendingApplication(dto.getBorrowerId());

        // Validate using validator component
        try {
            validator.validateCreate(dto, borrower, product, activeLoansCount, maxActiveLoans, hasPendingApplication);
        } catch (BusinessRuleException e) {
            spectraLogger.warn("LOAN_APPLICATION_CREATE_VALIDATION_FAILED")
                    .attr(BORROWER_ID, dto.getBorrowerId())
                    .attr(REASON, e.getMessage())
                    .log();
            throw e;
        } catch (ValidationException e) {
            spectraLogger.warn("LOAN_APPLICATION_CREATE_VALIDATION_ERROR")
                    .attr(BORROWER_ID, dto.getBorrowerId())
                    .attr(REASON, e.getMessage())
                    .log();
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

        spectraLogger.info("LOAN_APPLICATION_CREATE_SUCCESS")
                .attr(APPLICATION_ID, storedApplication.getId())
                .attr("applicationNumber", storedApplication.getApplicationNumber())
                .log();

        return mapper.toResponse(storedApplication);
    }

    public List<LoanApplicationResponseDTO> getApplicationsByBorrower(UUID borrowerId) {
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_LIST_BY_BORROWER_NOT_FOUND")
                            .attr(BORROWER_ID, borrowerId).log();
                    return new ResourceNotFoundException(BORROWER_NOT_FOUND);
                });

        List<LoanApplication> applications = applicationRepository.findByBorrowerId(borrowerId);
        List<LoanApplicationResponseDTO> result = applications.stream()
                .map(mapper::toResponse)
                .toList();

        spectraLogger.info("LOAN_APPLICATION_LIST_BY_BORROWER_SUCCESS")
                .attr(BORROWER_ID, borrowerId)
                .attr(COUNT, result.size())
                .log();
        return result;
    }

    public List<LoanApplicationResponseDTO> getApplicationsByStatus(String status, int page, int limit) {
        spectraLogger.info("LOAN_APPLICATION_LIST_BY_STATUS_ATTEMPT")
                .attr(STATUS, status)
                .attr("page", page)
                .attr("limit", limit)
                .log();
        try {
            LoanApplicationStatus applicationStatus = LoanApplicationStatus.valueOf(status.toUpperCase());
            List<LoanApplication> applications = applicationRepository.findByStatus(applicationStatus);
            List<LoanApplicationResponseDTO> result = applications.stream()
                    .skip( (long)(page - 1) * limit)
                    .limit(limit)
                    .map(mapper::toResponse)
                    .toList();
            spectraLogger.info("LOAN_APPLICATION_LIST_BY_STATUS_SUCCESS")
                    .attr(STATUS, status)
                    .attr(COUNT, result.size())
                    .log();
            return result;
        } catch (IllegalArgumentException e) {
            spectraLogger.warn("LOAN_APPLICATION_LIST_BY_STATUS_INVALID")
                    .attr(STATUS, status)
                    .log();
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
        spectraLogger.info("LOAN_APPLICATION_LIST_ALL_SUCCESS")
                .attr("page", page)
                .attr("limit", limit)
                .attr(COUNT, result.size())
                .log();
        return result;
    }

    public LoanApplicationResponseDTO getApplicationById(UUID id) {
        spectraLogger.info("LOAN_APPLICATION_FETCH_BY_ID_ATTEMPT").attr(APPLICATION_ID, id).log();
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_FETCH_BY_ID_NOT_FOUND").attr(APPLICATION_ID, id).log();
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });
        spectraLogger.info("LOAN_APPLICATION_FETCH_BY_ID_SUCCESS").attr(APPLICATION_ID, id).log();
        return mapper.toResponse(application);
    }

    public List<LoanApplicationResponseDTO> getPendingApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findPendingApplications();
        List<LoanApplicationResponseDTO> result = applications.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();
        spectraLogger.info("LOAN_APPLICATION_LIST_PENDING_SUCCESS")
                .attr(COUNT, result.size())
                .log();
        return result;
    }

    public List<LoanApplicationResponseDTO> getExpiredApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findExpiredApplications();
        List<LoanApplicationResponseDTO> result = applications.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();
        spectraLogger.info("LOAN_APPLICATION_LIST_EXPIRED_SUCCESS")
                .attr(COUNT, result.size())
                .log();
        return result;
    }

    @Transactional
    public LoanApplicationResponseDTO approveApplication(UUID id, BigDecimal approvedAmount) {
        spectraLogger.info("LOAN_APPLICATION_APPROVE_ATTEMPT")
                .attr(APPLICATION_ID, id)
                .attr(APPROVED_AMOUNT, approvedAmount)
                .log();

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_APPROVE_NOT_FOUND").attr(APPLICATION_ID, id).log();
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            spectraLogger.warn("LOAN_APPLICATION_APPROVE_STATUS_INVALID")
                    .attr(APPLICATION_ID, id)
                    .attr(CURRENT_STATUS, application.getStatus().name())
                    .log();
            throw new BusinessRuleException("Only pending or under-verification applications can be approved");
        }

        if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
            spectraLogger.warn("LOAN_APPLICATION_APPROVE_EXPIRED")
                    .attr(APPLICATION_ID, id)
                    .log();
            throw new BusinessRuleException("Application has expired. Please submit a new application");
        }

        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_APPROVE_PRODUCT_NOT_FOUND")
                            .attr(PRODUCT_ID, application.getProductId()).log();
                    return new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND);
                });

        // Validate using validator component
        try {
            validator.validateApproveAmount(approvedAmount, product);
        } catch (ValidationException e) {
            spectraLogger.warn("LOAN_APPLICATION_APPROVE_AMOUNT_OUT_OF_RANGE")
                    .attr(APPROVED_AMOUNT, approvedAmount)
                    .attr("min", product.getMinAmount())
                    .attr("max", product.getMaxAmount())
                    .log();
            throw e;
        }

        applicationRepository.approve(id, approvedAmount);
        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setApprovedAmount(approvedAmount);
        application.setApprovedAt(LocalDateTime.now());

        atroposEventPublisher.publishApplicationApprovedEvent(application);

        spectraLogger.info("LOAN_APPLICATION_APPROVE_SUCCESS")
                .attr(APPLICATION_ID, id)
                .attr(APPROVED_AMOUNT, approvedAmount)
                .log();

        return mapper.toResponse(application);
    }

    @Transactional
    public void rejectApplication(UUID id, String rejectionReason) {
        spectraLogger.info("LOAN_APPLICATION_REJECT_ATTEMPT")
                .attr(APPLICATION_ID, id)
                .attr(REASON, rejectionReason)
                .log();

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_REJECT_NOT_FOUND")
                            .attr(APPLICATION_ID, id).log();
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            spectraLogger.warn("LOAN_APPLICATION_REJECT_STATUS_INVALID")
                    .attr(APPLICATION_ID, id)
                    .attr(CURRENT_STATUS, application.getStatus().name())
                    .log();
            throw new BusinessRuleException("Only pending or under-verification applications can be rejected");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            spectraLogger.warn("LOAN_APPLICATION_REJECT_REASON_MISSING")
                    .attr(APPLICATION_ID, id)
                    .log();
            throw new ValidationException("Rejection reason is required");
        }

        applicationRepository.reject(id, rejectionReason);
        application = applicationRepository.findById(id).get();
        atroposEventPublisher.publishApplicationRejectedEvent(application, rejectionReason);

        spectraLogger.info("LOAN_APPLICATION_REJECT_SUCCESS")
                .attr(APPLICATION_ID, id)
                .log();
    }

    @Transactional
    public void cancelApplication(UUID id) {
        spectraLogger.info("LOAN_APPLICATION_CANCEL_ATTEMPT").attr(APPLICATION_ID, id).log();

        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_APPLICATION_CANCEL_NOT_FOUND").attr(APPLICATION_ID, id).log();
                    return new ResourceNotFoundException(LOAN_APPLICATION_NOT_FOUND);
                });

        if (application.getStatus() == LoanApplicationStatus.APPROVED ||
                application.getStatus() == LoanApplicationStatus.DISBURSED) {
            spectraLogger.warn("LOAN_APPLICATION_CANCEL_STATUS_INVALID")
                    .attr(APPLICATION_ID, id)
                    .attr(CURRENT_STATUS, application.getStatus().name())
                    .log();
            throw new BusinessRuleException("Approved or disbursed applications cannot be cancelled");
        }

        applicationRepository.cancel(id);

        spectraLogger.info("LOAN_APPLICATION_CANCEL_SUCCESS").attr(APPLICATION_ID, id).log();
    }

    private String generateApplicationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "APP-" + datePart + "-" + randomPart;
    }
}