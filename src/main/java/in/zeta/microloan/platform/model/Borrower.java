package in.zeta.microloan.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Borrower {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private LocalDate dob;
    private String address;
    private String idProofType;
    private String idProofNumber;
    private String employmentDetails;
    private String incomeDetails;
    private String profilePhotoUrl;
    private String digitalSignatureUrl;
    private Integer creditScore;
    private UserStatus status;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
