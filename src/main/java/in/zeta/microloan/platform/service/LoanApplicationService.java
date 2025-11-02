package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.LoanApplicationDTO;
import in.zeta.microloan.platform.dto.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanApplicationStatus;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanProductRepository productRepository;
    private final AtroposEventPublisherService atroposEventPublisher;


    @Value("${app.max-active-loans-per-borrower:3}")
    private int maxActiveLoans;

    @Value("${app.application-expiry-days:7}")
    private int applicationExpiryDays;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  BorrowerRepository borrowerRepository,
                                  LoanProductRepository productRepository, AtroposEventPublisherService atroposEventPublisher) {
        this.applicationRepository = applicationRepository;
        this.borrowerRepository = borrowerRepository;
        this.productRepository = productRepository;
        this.atroposEventPublisher = atroposEventPublisher;
    }

    @Transactional
    public LoanApplicationResponseDTO createApplication(LoanApplicationDTO dto) {
        // Validate borrower exists
        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        // Check if borrower is verified
        if (!borrower.getIsVerified()) {
            throw new BusinessRuleException("Borrower must be verified before applying for loan");
        }

        // Validate product exists and is active
        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        if (!"ACTIVE".equals(product.getStatus().name())) {
            throw new BusinessRuleException("Selected loan product is not active");
        }

        // Check if borrower has reached max active loans
        int activeLoansCount = borrowerRepository.countActiveLoansByBorrower(dto.getBorrowerId());
        if (activeLoansCount >= maxActiveLoans) {
            throw new BusinessRuleException("Maximum " + maxActiveLoans + " active loans allowed per borrower");
        }

        // Check if borrower has pending application
        if (applicationRepository.hasPendingApplication(dto.getBorrowerId())) {
            throw new BusinessRuleException("You already have a pending loan application");
        }

        // Validate requested amount
        if (dto.getRequestedAmount().compareTo(product.getMinAmount()) < 0 ||
                dto.getRequestedAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ValidationException(String.format("Loan amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }

        // Validate tenure if provided
        if (dto.getPreferredTenure() != null && dto.getPreferredTenure() > product.getTenureMonths()) {
            throw new ValidationException(String.format("Maximum tenure for this product is %d months",
                    product.getTenureMonths()));
        }

        // Create application
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

        return mapToResponseDTO(storedApplication);
    }

    public List<LoanApplicationResponseDTO> getApplicationsByBorrower(Long borrowerId) {
        // Validate borrower exists
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        List<LoanApplication> applications = applicationRepository.findByBorrowerId(borrowerId);
        return applications.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanApplicationResponseDTO> getApplicationsByStatus(String status, int page, int limit) {
        try {
            LoanApplicationStatus applicationStatus = LoanApplicationStatus.valueOf(status.toUpperCase());
            List<LoanApplication> applications = applicationRepository.findByStatus(applicationStatus);
            return applications.stream()
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid application status: " + status);
        }
    }

    public List<LoanApplicationResponseDTO> getAllApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findAll();
        return applications.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public LoanApplicationResponseDTO getApplicationById(Long id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));
        return mapToResponseDTO(application);
    }

    public LoanApplicationResponseDTO getLatestApplicationByBorrower(Long borrowerId) {
        // Validate borrower exists
        borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        LoanApplication application = applicationRepository.findLatestByBorrowerId(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("No applications found for this borrower"));
        return mapToResponseDTO(application);
    }

    public List<LoanApplicationResponseDTO> getPendingApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findPendingApplications();
        return applications.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanApplicationResponseDTO> getExpiredApplications(int page, int limit) {
        List<LoanApplication> applications = applicationRepository.findExpiredApplications();
        return applications.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanApplicationResponseDTO approveApplication(Long id, Long approvedBy, BigDecimal approvedAmount) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW &&
                application.getStatus() != LoanApplicationStatus.UNDER_VERIFICATION) {
            throw new BusinessRuleException("Only pending or under-verification applications can be approved");
        }

        // Check if application has expired
        if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
            throw new BusinessRuleException("Application has expired. Please submit a new application");
        }

        // Validate approved amount
        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        if (approvedAmount.compareTo(product.getMinAmount()) < 0 ||
                approvedAmount.compareTo(product.getMaxAmount()) > 0) {
            throw new ValidationException(String.format("Approved amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }

        applicationRepository.approve(id, approvedBy, approvedAmount);
        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setApprovedAmount(approvedAmount);
        application.setApprovedBy(approvedBy);
        application.setApprovedAt(LocalDateTime.now());

        atroposEventPublisher.publishApplicationApprovedEvent(application, approvedBy);

        return mapToResponseDTO(application);
    }

    @Transactional
    public void rejectApplication(Long id, String rejectionReason) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW &&
                application.getStatus() != LoanApplicationStatus.UNDER_VERIFICATION) {
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
    public LoanApplicationResponseDTO moveToVerification(Long id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("Only pending applications can be moved to verification");
        }

        applicationRepository.updateStatus(id, LoanApplicationStatus.UNDER_VERIFICATION);
        application.setStatus(LoanApplicationStatus.UNDER_VERIFICATION);

        return mapToResponseDTO(application);
    }

    @Transactional
    public void cancelApplication(Long id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() == LoanApplicationStatus.APPROVED ||
                application.getStatus() == LoanApplicationStatus.DISBURSED) {
            throw new BusinessRuleException("Approved or disbursed applications cannot be cancelled");
        }

        applicationRepository.delete(id);
    }

    private String generateApplicationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "APP-" + datePart + "-" + randomPart;
    }

    private LoanApplicationResponseDTO mapToResponseDTO(LoanApplication application) {
        return LoanApplicationResponseDTO.builder()
                .id(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .borrowerId(application.getBorrowerId())
                .productId(application.getProductId())
                .requestedAmount(application.getRequestedAmount())
                .purpose(application.getPurpose())
                .preferredTenure(application.getPreferredTenure())
                .status(application.getStatus().name())
                .approvedAmount(application.getApprovedAmount())
                .approvedBy(application.getApprovedBy())
                .approvedAt(application.getApprovedAt())
                .rejectionReason(application.getRejectionReason())
                .expiresAt(application.getExpiresAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}