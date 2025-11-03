package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.BorrowerMapper;
import in.zeta.microloan.platform.service.validator.BorrowerValidator;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BorrowerService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(BorrowerService.class);

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
        spectraLogger.info("BORROWER_REGISTER_ATTEMPT")
                .attr("phone", dto.getPhone())
                .attr("name", dto.getName())
                .log();

        validator.validateRegistration(dto);

        if (dto.getHouseholdId() != null) {
            var householdOpt = householdRepository.findById(dto.getHouseholdId());
            if (householdOpt.isEmpty()) {
                spectraLogger.warn("BORROWER_REGISTER_HOUSEHOLD_NOT_FOUND")
                        .attr("householdId", dto.getHouseholdId())
                        .log();
                throw new ResourceNotFoundException("Household not found");
            }
            if (!Boolean.TRUE.equals(householdOpt.get().getIsVerified())) {
                spectraLogger.warn("BORROWER_REGISTER_HOUSEHOLD_NOT_VERIFIED")
                        .attr("householdId", dto.getHouseholdId())
                        .log();
                throw new ResourceNotFoundException("Household not verified");
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

        spectraLogger.info("BORROWER_REGISTER_SUCCESS")
                .attr("borrowerId", savedBorrower.getId())
                .attr("phone", savedBorrower.getPhone())
                .log();

        return mapper.toResponse(savedBorrower);
    }

    public BorrowerResponseDTO getBorrowerById(UUID id) {
        spectraLogger.info("BORROWER_FETCH_BY_ID_ATTEMPT").attr("borrowerId", id).log();
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_FETCH_BY_ID_NOT_FOUND").attr("borrowerId", id).log();
                    return new ResourceNotFoundException("Borrower not found with ID: " + id);
                });
        spectraLogger.info("BORROWER_FETCH_BY_ID_SUCCESS").attr("borrowerId", borrower.getId()).log();
        return mapper.toResponse(borrower);
    }

    public BorrowerResponseDTO getBorrowerByPhone(String phone) {
        spectraLogger.info("BORROWER_FETCH_BY_PHONE_ATTEMPT").attr("phone", phone).log();
        Borrower borrower = borrowerRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_FETCH_BY_PHONE_NOT_FOUND").attr("phone", phone).log();
                    return new ResourceNotFoundException("Borrower not found with phone: " + phone);
                });
        spectraLogger.info("BORROWER_FETCH_BY_PHONE_SUCCESS").attr("borrowerId", borrower.getId()).log();
        return mapper.toResponse(borrower);
    }

    public List<BorrowerResponseDTO> getBorrowersByHousehold(UUID householdId) {
        spectraLogger.info("BORROWERS_FETCH_BY_HOUSEHOLD_ATTEMPT").attr("householdId", householdId).log();
        householdRepository.findById(householdId)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWERS_FETCH_BY_HOUSEHOLD_NOT_FOUND").attr("householdId", householdId).log();
                    return new ResourceNotFoundException("Household not found");
                });

        List<Borrower> borrowers = borrowerRepository.findByHouseholdId(householdId);
        List<BorrowerResponseDTO> result = borrowers.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        spectraLogger.info("BORROWERS_FETCH_BY_HOUSEHOLD_SUCCESS")
                .attr("householdId", householdId)
                .attr("count", result.size())
                .log();
        return result;
    }

    public List<BorrowerResponseDTO> getAllBorrowers(String status, int page, int limit) {
        spectraLogger.info("BORROWERS_LIST_REQUEST")
                .attr("statusFilter", status)
                .attr("page", page)
                .attr("limit", limit)
                .log();

        List<Borrower> borrowers;
        if (status != null && !status.isEmpty()) {
            try {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                borrowers = borrowerRepository.findByStatus(userStatus);
            } catch (IllegalArgumentException e) {
                spectraLogger.warn("BORROWERS_LIST_INVALID_STATUS").attr("status", status).log();
                throw new ResourceNotFoundException("Invalid status: " + status);
            }
        } else {
            borrowers = borrowerRepository.findAll();
        }

        List<BorrowerResponseDTO> result = borrowers.stream()
                .skip((page - 1) * limit)
                .limit(limit)
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        spectraLogger.info("BORROWERS_LIST_RESPONSE")
                .attr("returnedCount", result.size())
                .log();
        return result;
    }

    @Transactional
    public BorrowerResponseDTO updateBorrower(UUID id, BorrowerUpdateRequestDTO dto) {
        spectraLogger.info("BORROWER_UPDATE_ATTEMPT").attr("borrowerId", id).log();

        validator.validateUpdate(dto);

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_UPDATE_NOT_FOUND").attr("borrowerId", id).log();
                    return new ResourceNotFoundException("Borrower not found");
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

        spectraLogger.info("BORROWER_UPDATE_SUCCESS").attr("borrowerId", id).log();
        return mapper.toResponse(borrower);
    }

    @Transactional
    public BorrowerResponseDTO verifyBorrower(UUID id) {
        spectraLogger.info("BORROWER_VERIFY_ATTEMPT").attr("borrowerId", id).log();

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_VERIFY_NOT_FOUND").attr("borrowerId", id).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        validator.validateVerification(borrower);

        borrower.setIsVerified(true);
        borrowerRepository.update(borrower);

        spectraLogger.info("BORROWER_VERIFY_SUCCESS").attr("borrowerId", id).log();
        return mapper.toResponse(borrower);
    }

    @Transactional
    public BorrowerResponseDTO updateBorrowerStatus(UUID id, String statusStr) {
        spectraLogger.info("BORROWER_STATUS_UPDATE_ATTEMPT")
                .attr("borrowerId", id)
                .attr("newStatus", statusStr)
                .log();

        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_STATUS_UPDATE_NOT_FOUND").attr("borrowerId", id).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        UserStatus status = validator.validateStatusChange(id, statusStr);

        borrower.setStatus(status);
        borrowerRepository.update(borrower);

        spectraLogger.info("BORROWER_STATUS_UPDATE_SUCCESS")
                .attr("borrowerId", id)
                .attr("newStatus", status.name())
                .log();
        return mapper.toResponse(borrower);
    }

    @Transactional
    public void deleteBorrower(UUID id) {
        spectraLogger.info("BORROWER_DELETE_ATTEMPT").attr("borrowerId", id).log();

        borrowerRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_DELETE_NOT_FOUND").attr("borrowerId", id).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        validator.validateDeletion(id);

        borrowerRepository.delete(id);
        spectraLogger.info("BORROWER_DELETE_SUCCESS").attr("borrowerId", id).log();
    }

    public BorrowerCreditSummaryResponseDTO getBorrowerCreditSummary(UUID borrowerId) {
        spectraLogger.info("BORROWER_CREDIT_SUMMARY_REQUEST").attr("borrowerId", borrowerId).log();

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    spectraLogger.warn("BORROWER_CREDIT_SUMMARY_NOT_FOUND").attr("borrowerId", borrowerId).log();
                    return new ResourceNotFoundException("Borrower not found");
                });

        int totalLoans = borrowerRepository.countAllLoansByBorrower(borrowerId);
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrowerId);
        int closedLoans = borrowerRepository.countClosedLoansByBorrower(borrowerId);
        BigDecimal totalDisbursed = borrowerRepository.getTotalDisbursedAmount(borrowerId);
        BigDecimal totalOutstanding = borrowerRepository.getTotalOutstandingAmount(borrowerId);
        BigDecimal totalPaid = borrowerRepository.getTotalPaidAmount(borrowerId);

        spectraLogger.info("BORROWER_CREDIT_SUMMARY_GENERATED")
                .attr("borrowerId", borrowerId)
                .attr("totalLoans", totalLoans)
                .attr("activeLoans", activeLoans)
                .attr("closedLoans", closedLoans)
                .log();

        return mapper.toCreditSummary(borrower, totalLoans, activeLoans, closedLoans,
                totalDisbursed, totalOutstanding, totalPaid);
    }
}