package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.HouseholdRegistrationDTO;
import in.zeta.microloan.platform.dto.HouseholdResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    public HouseholdService(HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    @Transactional
    public HouseholdResponseDTO createHousehold(HouseholdRegistrationDTO dto) {
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

        Long householdId = householdRepository.create(household);
        household.setId(householdId);

        return mapToResponseDTO(household);
    }

    public HouseholdResponseDTO getHouseholdById(Long id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
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
