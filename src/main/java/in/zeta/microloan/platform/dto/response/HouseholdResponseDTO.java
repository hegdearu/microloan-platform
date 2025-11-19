package in.zeta.microloan.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HouseholdResponseDTO {
    private UUID id;
    private String householdNumber;
    private String primaryAddress;
    private String city;
    private String state;
    private BigDecimal totalAnnualIncome;
    private Integer totalMembers;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
