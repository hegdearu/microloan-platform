package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Borrower {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private LocalDate dob;
    private UUID householdId;
    private String relationshipToHead;
    private Boolean isHouseholdHead;
    private BigDecimal individualAnnualIncome;
    private String occupation;
    private String address;
    private String idProofType;
    private String idProofNumber;
    private String employmentDetails;
    private String incomeDetails;
    private String profilePhotoUrl;
    private Integer creditScore;
    private UserStatus status;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
