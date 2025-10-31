package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.LoanApplicationDTO;
import in.zeta.microloan.platform.dto.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanApplicationStatus;
import in.zeta.microloan.platform.model.LoanProduct;
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

    @Value("${app.max-active-loans-per-borrower:3}")
    private int maxActiveLoans;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  BorrowerRepository borrowerRepository,
                                  LoanProductRepository productRepository) {
        this.applicationRepository = applicationRepository;
        this.borrowerRepository = borrowerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public LoanApplicationResponseDTO createApplication(LoanApplicationDTO dto) {
        // Validate borrower exists
        borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        // Validate product exists
        LoanProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        // Check if borrower has reached max active loans
//        int activeLoansCount = borrowerRepository.countActiveLoans(dto.getBorrowerId());
//        if (activeLoansCount >= maxActiveLoans) {
//            throw new BusinessRuleException("Maximum " + maxActiveLoans + " active loans allowed per borrower");
//        }

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

        // Create application
        LoanApplication application = LoanApplication.builder()
                .applicationNumber(generateApplicationNumber())
                .borrowerId(dto.getBorrowerId())
                .productId(dto.getProductId())
                .requestedAmount(dto.getRequestedAmount())
                .purpose(dto.getPurpose())
                .preferredTenure(dto.getPreferredTenure())
                .status(LoanApplicationStatus.PENDING_REVIEW)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        LoanApplication storedApplication = applicationRepository.create(application);


        return mapToResponseDTO(storedApplication);
    }

    public List<LoanApplicationResponseDTO> getApplicationsByBorrower(Long borrowerId) {
        List<LoanApplication> applications = applicationRepository.findByBorrowerId(borrowerId);
        return applications.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public LoanApplicationResponseDTO getApplicationById(Long id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));
        return mapToResponseDTO(application);
    }

    @Transactional
    public LoanApplicationResponseDTO approveApplication(Long id, Long approvedBy, BigDecimal approvedAmount) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW &&
                application.getStatus() != LoanApplicationStatus.UNDER_VERIFICATION) {
            throw new BusinessRuleException("Only pending applications can be approved");
        }

        applicationRepository.approve(id, approvedBy, approvedAmount);
        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setApprovedAmount(approvedAmount);

        return mapToResponseDTO(application);
    }

    @Transactional
    public void rejectApplication(Long id, String rejectionReason) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));

        if (application.getStatus() != LoanApplicationStatus.PENDING_REVIEW &&
                application.getStatus() != LoanApplicationStatus.UNDER_VERIFICATION) {
            throw new BusinessRuleException("Only pending applications can be rejected");
        }

        applicationRepository.reject(id, rejectionReason);
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
                .requestedAmount(application.getRequestedAmount())
                .purpose(application.getPurpose())
                .status(application.getStatus().name())
                .requestedAmount(application.getApprovedAmount())
                .expiresAt(application.getExpiresAt())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
