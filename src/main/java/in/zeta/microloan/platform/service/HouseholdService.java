package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.HouseholdMapper;
import in.zeta.microloan.platform.service.validator.HouseholdValidator;
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

        validator.validateRegistration(dto);

        Household household = Household.builder()
                .householdNumber(generateHouseholdNumber())
                .primaryAddress(dto.getPrimaryAddress())
                .pincode(dto.getPincode())
                .city(dto.getCity())
                .state(dto.getState())
                .totalAnnualIncome(dto.getTotalAnnualIncome())
                .incomeProofType(dto.getIncomeProofType())
                .totalMembers(dto.getTotalMembers())
                .householdType(dto.getHouseholdType())
                .isVerified(false)
                .build();

        Household storedHousehold = householdRepository.create(household);

        return mapper.toResponse(storedHousehold);
    }

    public HouseholdResponseDTO getHouseholdById(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(HOUSEHOLD_NOT_FOUND);
                });
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