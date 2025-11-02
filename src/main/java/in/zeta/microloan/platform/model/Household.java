package in.zeta.microloan.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Household {
    private Long id;
    private String householdNumber;
    private String primaryAddress;
    private String pincode;
    private String city;
    private String state;
    private BigDecimal totalAnnualIncome;
    private String incomeProofType;
    private String incomeProofUrl;
    private LocalDate incomeVerifiedDate;
    private Integer totalMembers;
    private String householdType;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
