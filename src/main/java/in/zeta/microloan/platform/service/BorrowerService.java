package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.exception.Error;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.BorrowerMapper;
import in.zeta.microloan.platform.service.validator.BorrowerValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.BORROWER_ID;
import static in.zeta.microloan.platform.constants.LogConstants.HOUSEHOLD_ID;
import static in.zeta.microloan.platform.exception.Error.BORROWER_NOT_FOUND_WITH_ID;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;
    private final BorrowerValidator validator;
    private final BorrowerMapper mapper;

    public BorrowerService(BorrowerRepository borrowerRepository,
                           HouseholdRepository householdRepository,
                           BorrowerValidator validator,
                           BorrowerMapper mapper) {
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public BorrowerResponseDTO registerBorrower(BorrowerRegistrationRequestDTO dto) {

        validator.validateRegistration(dto);

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

        return mapper.toResponse(savedBorrower);
    }

    public BorrowerResponseDTO getBorrowerById(UUID id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(BORROWER_NOT_FOUND_WITH_ID);
                });
        return mapper.toResponse(borrower);
    }

    public BorrowerResponseDTO getBorrowerByPhone(String phone) {
        Borrower borrower = borrowerRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(Error.BORROWER_NOT_FOUND_WITH_PHONE);
                });
        return mapper.toResponse(borrower);
    }

    public List<BorrowerResponseDTO> getBorrowersByHousehold(UUID householdId) {
        householdRepository.findById(householdId)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(Error.HOUSEHOLD_NOT_FOUND);
                });

        List<Borrower> borrowers = borrowerRepository.findByHouseholdId(householdId);
        List<BorrowerResponseDTO> result = borrowers.stream()
                .map(mapper::toResponse)
                .toList();

        return result;
    }

    public List<BorrowerResponseDTO> getAllBorrowers(String status, int page, int limit) {

        List<Borrower> borrowers;
        if (status != null && !status.isEmpty()) {
            try {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                borrowers = borrowerRepository.findByStatus(userStatus);
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException(Error.INVALID_STATUS);
            }
        } else {
            borrowers = borrowerRepository.findAll();
        }

        List<BorrowerResponseDTO> result = borrowers.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .toList();

        return result;
    }

    @Transactional
    public BorrowerResponseDTO updateBorrower(UUID id, BorrowerUpdateRequestDTO dto) {

        validator.validateUpdate(dto);

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(in.zeta.microloan.platform.exception.Error.BORROWER_NOT_FOUND);
                });

        if (dto.getName() != null) borrower.setName(dto.getName());
        if (dto.getEmail() != null) borrower.setEmail(dto.getEmail());
        if (dto.getAddress() != null) borrower.setAddress(dto.getAddress());
        if (dto.getOccupation() != null) borrower.setOccupation(dto.getOccupation());
        if (dto.getIndividualAnnualIncome() != null)
            borrower.setIndividualAnnualIncome(dto.getIndividualAnnualIncome());
        if (dto.getEmploymentDetails() != null)
            borrower.setEmploymentDetails(dto.getEmploymentDetails());
        if (dto.getIncomeDetails() != null)
            borrower.setIncomeDetails(dto.getIncomeDetails());

        borrowerRepository.update(borrower);

        return mapper.toResponse(borrower);
    }

    @Transactional
    public BorrowerResponseDTO verifyBorrower(UUID id) {

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(in.zeta.microloan.platform.exception.Error.BORROWER_NOT_FOUND);
                });

        validator.validateVerification(borrower);

        borrower.setIsVerified(true);
        borrowerRepository.update(borrower);

        return mapper.toResponse(borrower);
    }

    @Transactional
    public BorrowerResponseDTO updateBorrowerStatus(UUID id, String statusStr) {

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(Error.BORROWER_NOT_FOUND);
                });

        UserStatus status = validator.validateStatusChange(id, statusStr);

        borrower.setStatus(status);
        borrowerRepository.update(borrower);

        return mapper.toResponse(borrower);
    }

    public BorrowerCreditSummaryResponseDTO getBorrowerCreditSummary(UUID borrowerId) {

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(Error.BORROWER_NOT_FOUND);
                });

        int totalLoans = borrowerRepository.countAllLoansByBorrower(borrowerId);
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrowerId);
        int closedLoans = borrowerRepository.countClosedLoansByBorrower(borrowerId);
        BigDecimal totalDisbursed = borrowerRepository.getTotalDisbursedAmount(borrowerId);
        BigDecimal totalOutstanding = borrowerRepository.getTotalOutstandingAmount(borrowerId);
        BigDecimal totalPaid = borrowerRepository.getTotalPaidAmount(borrowerId);

        return mapper.toCreditSummary(borrower, totalLoans, activeLoans, closedLoans,
                totalDisbursed, totalOutstanding, totalPaid);
    }
}