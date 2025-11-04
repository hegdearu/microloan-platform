package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.HouseholdMapper;
import in.zeta.microloan.platform.service.validator.HouseholdValidator;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

import static in.zeta.microloan.platform.exception.Error.HOUSEHOLD_NOT_FOUND;

@Service
public class HouseholdService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(HouseholdService.class);

    private final HouseholdRepository householdRepository;
    private final HouseholdValidator validator;
    private final HouseholdMapper mapper;

    public HouseholdService(HouseholdRepository householdRepository,
                            HouseholdValidator validator,
                            HouseholdMapper mapper) {
        this.householdRepository = householdRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public HouseholdResponseDTO createHousehold(HouseholdRegistrationRequestDTO dto) {
        spectraLogger.info("HOUSEHOLD_CREATE_ATTEMPT")
                .attr("city", dto.getCity())
                .attr("state", dto.getState())
                .log();

        validator.validateRegistration(dto);

        Household household = Household.builder()
                .householdNumber(generateHouseholdNumber())
                .primaryAddress(dto.getPrimaryAddress())
                .pincode(dto.getPincode())
                .city(dto.getCity())
                .state(dto.getState())
                .totalAnnualIncome(dto.getTotalAnnualIncome())
                .incomeProofType(dto.getIncomeProofType())
                .totalMembers(1)
                .householdType(dto.getHouseholdType())
                .isVerified(false)
                .build();

        Household stored = householdRepository.create(household);

        spectraLogger.info("HOUSEHOLD_CREATE_SUCCESS")
                .attr("householdId", stored.getId())
                .attr("householdNumber", stored.getHouseholdNumber())
                .log();

        return mapper.toResponse(stored);
    }

    public HouseholdResponseDTO getHouseholdById(UUID id) {
        spectraLogger.info("HOUSEHOLD_FETCH_BY_ID_ATTEMPT").attr("householdId", id).log();
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("HOUSEHOLD_FETCH_BY_ID_NOT_FOUND").attr("householdId", id).log();
                    return new ResourceNotFoundException(HOUSEHOLD_NOT_FOUND);
                });
        spectraLogger.info("HOUSEHOLD_FETCH_BY_ID_SUCCESS").attr("householdId", id).log();
        return mapper.toResponse(household);
    }

    public HouseholdResponseDTO verifyHousehold(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        if (Boolean.TRUE.equals(household.getIsVerified())) {
            return mapper.toResponse(household);
        }

        household.setIsVerified(true);
        household.setIncomeVerifiedDate(LocalDate.now());
        householdRepository.update(household);
        return mapper.toResponse(household);
    }

    private String generateHouseholdNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "HH-" + datePart + "-" + randomPart;
    }
}