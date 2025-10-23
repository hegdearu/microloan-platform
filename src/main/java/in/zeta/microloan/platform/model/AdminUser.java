package in.zeta.microloan.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private AdminRole role;
    private String fullName;
    private String phone;
    private UserStatus status;
    private LocalDateTime lastLogin;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}