package in.zeta.microloan.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerRegistrationResponse {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String message;
    private LocalDateTime registeredAt;
}
