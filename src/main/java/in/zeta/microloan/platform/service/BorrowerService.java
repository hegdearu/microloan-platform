package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.BorrowerRegistrationDTO;
import in.zeta.microloan.platform.dto.BorrowerResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;

    @Value("${app.min-age-requirement:18}")
    private int minAgeRequirement;

    public BorrowerService(BorrowerRepository borrowerRepository,
                           HouseholdRepository householdRepository) {
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
    }

    @Transactional
    public BorrowerResponseDTO registerBorrower(BorrowerRegistrationDTO dto) {
        int age = Period.between(dto.getDob(), LocalDate.now()).getYears();
        if (age < minAgeRequirement) {
            throw new ValidationException("Borrower must be at least " + minAgeRequirement + " years old");
        }

        if (borrowerRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new ValidationException("Phone number already registered");
        }

        if (dto.getHouseholdId() != null) {
            householdRepository.findById(dto.getHouseholdId())
                    .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        }

        Borrower borrower = Borrower.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .dob(dto.getDob())
                .householdId(dto.getHouseholdId())
                .relationshipToHead(dto.getRelationshipToHead())
                .isHouseholdHead(dto.getIsHouseholdHead())
                .individualAnnualIncome(dto.getIndividualAnnualIncome())
                .occupation(dto.getOccupation())
                .address(dto.getAddress())
                .idProofType(dto.getIdProofType())
                .idProofNumber(dto.getIdProofNumber())
                .employmentDetails(dto.getEmploymentDetails())
                .incomeDetails(dto.getIncomeDetails())
                .status(UserStatus.ACTIVE)
                .isVerified(true)
                .build();

        Long borrowerId = borrowerRepository.create(borrower);
        borrower.setId(borrowerId);

        return mapToResponseDTO(borrower);
    }

    public BorrowerResponseDTO getBorrowerById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));
        return mapToResponseDTO(borrower);
    }

    private BorrowerResponseDTO mapToResponseDTO(Borrower borrower) {
        return BorrowerResponseDTO.builder()
                .id(borrower.getId())
                .name(borrower.getName())
                .phone(borrower.getPhone())
                .email(borrower.getEmail())
                .householdId(borrower.getHouseholdId())
                .individualAnnualIncome(borrower.getIndividualAnnualIncome())
                .occupation(borrower.getOccupation())
                .status(borrower.getStatus())
                .isVerified(borrower.getIsVerified())
                .createdAt(borrower.getCreatedAt())
                .build();
    }
}
