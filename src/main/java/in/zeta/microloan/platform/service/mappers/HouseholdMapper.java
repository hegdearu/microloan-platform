package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.model.Household;
import org.springframework.stereotype.Component;

@Component
public class HouseholdMapper {
    public HouseholdResponseDTO toResponse(Household h) {
        return HouseholdResponseDTO.builder()
                .id(h.getId())
                .householdNumber(h.getHouseholdNumber())
                .primaryAddress(h.getPrimaryAddress())
                .city(h.getCity())
                .state(h.getState())
                .totalAnnualIncome(h.getTotalAnnualIncome())
                .totalMembers(h.getTotalMembers())
                .isVerified(h.getIsVerified())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
