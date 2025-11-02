package in.zeta.microloan.platform.dto;
import in.zeta.microloan.platform.model.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BorrowerResponseDTO {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private LocalDate dob;
    private Long householdId;
    private String relationshipToHead;
    private Boolean isHouseholdHead;
    private BigDecimal individualAnnualIncome;
    private String occupation;
    private String address;
    private String idProofType;
    private String idProofNumber;
    private Integer creditScore;
    private UserStatus status;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
