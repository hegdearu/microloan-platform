package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class HouseholdService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(HouseholdService.class);

    private final HouseholdRepository householdRepository;

    public HouseholdService(HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    @Transactional
    public HouseholdResponseDTO createHousehold(HouseholdRegistrationRequestDTO dto) {
        spectraLogger.info("HOUSEHOLD_CREATE_ATTEMPT")
                .attr("city", dto.getCity())
                .attr("state", dto.getState())
                .log();

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

        return mapToResponseDTO(stored);
    }

    public HouseholdResponseDTO getHouseholdById(Long id) {
        spectraLogger.info("HOUSEHOLD_FETCH_BY_ID_ATTEMPT").attr("householdId", id).log();
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("HOUSEHOLD_FETCH_BY_ID_NOT_FOUND").attr("householdId", id).log();
                    return new ResourceNotFoundException("Household not found");
                });
        spectraLogger.info("HOUSEHOLD_FETCH_BY_ID_SUCCESS").attr("householdId", id).log();
        return mapToResponseDTO(household);
    }

    private String generateHouseholdNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "HH-" + datePart + "-" + randomPart;
    }

    private HouseholdResponseDTO mapToResponseDTO(Household household) {
        return HouseholdResponseDTO.builder()
                .id(household.getId())
                .householdNumber(household.getHouseholdNumber())
                .primaryAddress(household.getPrimaryAddress())
                .city(household.getCity())
                .state(household.getState())
                .totalAnnualIncome(household.getTotalAnnualIncome())
                .totalMembers(household.getTotalMembers())
                .isVerified(household.getIsVerified())
                .createdAt(household.getCreatedAt())
                .build();
    }
}