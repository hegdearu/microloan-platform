package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.*;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;
    private final LoanRepository loanRepository;

    @Value("${app.min-age-requirement:18}")
    private int minAgeRequirement;

    public BorrowerService(BorrowerRepository borrowerRepository,
                           HouseholdRepository householdRepository,
                           LoanRepository loanRepository) {
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public BorrowerResponseDTO registerBorrower(BorrowerRegistrationDTO dto) {
        // Validate age
        int age = Period.between(dto.getDob(), LocalDate.now()).getYears();
        if (age < minAgeRequirement) {
            throw new ValidationException("Borrower must be at least " + minAgeRequirement + " years old");
        }

        // Check if phone already exists
        if (borrowerRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new ValidationException("Phone number already registered");
        }

        // Validate household if provided
        if (dto.getHouseholdId() != null) {
            householdRepository.findById(dto.getHouseholdId())
                    .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        }

        // Validate email format if provided
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            if (!isValidEmail(dto.getEmail())) {
                throw new ValidationException("Invalid email format");
            }
        }

        Borrower borrower = Borrower.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .dob(dto.getDob())
                .householdId(dto.getHouseholdId())
                .relationshipToHead(dto.getRelationshipToHead())
                .isHouseholdHead(dto.getIsHouseholdHead() != null ? dto.getIsHouseholdHead() : false)
                .individualAnnualIncome(dto.getIndividualAnnualIncome())
                .occupation(dto.getOccupation())
                .address(dto.getAddress())
                .idProofType(dto.getIdProofType())
                .idProofNumber(dto.getIdProofNumber())
                .employmentDetails(dto.getEmploymentDetails())
                .incomeDetails(dto.getIncomeDetails())
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .build();

        Borrower savedBorrower = borrowerRepository.create(borrower);

        return mapToResponseDTO(savedBorrower);
    }

    public BorrowerResponseDTO getBorrowerById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with ID: " + id));
        return mapToResponseDTO(borrower);
    }

    public BorrowerResponseDTO getBorrowerByPhone(String phone) {
        Borrower borrower = borrowerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with phone: " + phone));
        return mapToResponseDTO(borrower);
    }

    public List<BorrowerResponseDTO> getBorrowersByHousehold(Long householdId) {
        // Validate household exists
        householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));

        List<Borrower> borrowers = borrowerRepository.findByHouseholdId(householdId);
        return borrowers.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BorrowerResponseDTO> getAllBorrowers(String status, int page, int limit) {
        List<Borrower> borrowers;

        if (status != null && !status.isEmpty()) {
            try {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                borrowers = borrowerRepository.findByStatus(userStatus);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + status);
            }
        } else {
            borrowers = borrowerRepository.findAll();
        }

        return borrowers.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BorrowerResponseDTO updateBorrower(Long id, BorrowerUpdateDTO dto) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        // Update only provided fields
        if (dto.getName() != null) {
            borrower.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            if (!isValidEmail(dto.getEmail())) {
                throw new ValidationException("Invalid email format");
            }
            borrower.setEmail(dto.getEmail());
        }
        if (dto.getAddress() != null) {
            borrower.setAddress(dto.getAddress());
        }
        if (dto.getOccupation() != null) {
            borrower.setOccupation(dto.getOccupation());
        }
        if (dto.getIndividualAnnualIncome() != null) {
            borrower.setIndividualAnnualIncome(dto.getIndividualAnnualIncome());
        }
        if (dto.getEmploymentDetails() != null) {
            borrower.setEmploymentDetails(dto.getEmploymentDetails());
        }
        if (dto.getIncomeDetails() != null) {
            borrower.setIncomeDetails(dto.getIncomeDetails());
        }

        borrowerRepository.update(borrower);

        return mapToResponseDTO(borrower);
    }

    @Transactional
    public BorrowerResponseDTO verifyBorrower(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        if (borrower.getIsVerified()) {
            throw new BusinessRuleException("Borrower is already verified");
        }

        borrower.setIsVerified(true);
        borrowerRepository.update(borrower);

        return mapToResponseDTO(borrower);
    }

    @Transactional
    public BorrowerResponseDTO updateBorrowerStatus(Long id, String statusStr) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        UserStatus status;
        try {
            status = UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + statusStr);
        }

        // Check if borrower has active loans before suspending/deactivating
        if (status == UserStatus.SUSPENDED || status == UserStatus.INACTIVE) {
            int activeLoans = borrowerRepository.countActiveLoansByBorrower(id);
            if (activeLoans > 0) {
                throw new BusinessRuleException(
                        "Cannot change status. Borrower has " + activeLoans + " active loan(s)");
            }
        }

        borrower.setStatus(status);
        borrowerRepository.update(borrower);

        return mapToResponseDTO(borrower);
    }

    @Transactional
    public void deleteBorrower(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        // Check if borrower has any loans
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(id);
        if (activeLoans > 0) {
            throw new BusinessRuleException(
                    "Cannot delete borrower with active loans. Please close all loans first.");
        }

        borrowerRepository.delete(id);
    }

    public BorrowerCreditSummaryDTO getBorrowerCreditSummary(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

        int totalLoans = borrowerRepository.countAllLoansByBorrower(borrowerId);
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrowerId);
        int closedLoans = borrowerRepository.countClosedLoansByBorrower(borrowerId);
        BigDecimal totalDisbursed = borrowerRepository.getTotalDisbursedAmount(borrowerId);
        BigDecimal totalOutstanding = borrowerRepository.getTotalOutstandingAmount(borrowerId);
        BigDecimal totalPaid = borrowerRepository.getTotalPaidAmount(borrowerId);

        return BorrowerCreditSummaryDTO.builder()
                .borrowerId(borrowerId)
                .borrowerName(borrower.getName())
                .totalLoans(totalLoans)
                .activeLoans(activeLoans)
                .closedLoans(closedLoans)
                .totalDisbursed(totalDisbursed != null ? totalDisbursed : BigDecimal.ZERO)
                .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                .totalPaid(totalPaid != null ? totalPaid : BigDecimal.ZERO)
                .creditScore(borrower.getCreditScore())
                .isVerified(borrower.getIsVerified())
                .status(borrower.getStatus().name())
                .build();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    private BorrowerResponseDTO mapToResponseDTO(Borrower borrower) {
        return BorrowerResponseDTO.builder()
                .id(borrower.getId())
                .name(borrower.getName())
                .phone(borrower.getPhone())
                .email(borrower.getEmail())
                .dob(borrower.getDob())
                .householdId(borrower.getHouseholdId())
                .relationshipToHead(borrower.getRelationshipToHead())
                .isHouseholdHead(borrower.getIsHouseholdHead())
                .individualAnnualIncome(borrower.getIndividualAnnualIncome())
                .occupation(borrower.getOccupation())
                .address(borrower.getAddress())
                .idProofType(borrower.getIdProofType())
                .idProofNumber(borrower.getIdProofNumber())
                .creditScore(borrower.getCreditScore())
                .status(borrower.getStatus())
                .isVerified(borrower.getIsVerified())
                .createdAt(borrower.getCreatedAt())
                .updatedAt(borrower.getUpdatedAt())
                .build();
    }
}
