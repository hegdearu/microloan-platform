package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.NotificationChannel;
import in.zeta.microloan.platform.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private Long recipientId;
    private String recipientType; // 'borrower' or 'admin'
    private NotificationChannel channel;
    private String templateId;
    private String subject;
    private String content;
    private NotificationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private Integer retryCount;
    private String metadata; // JSON string
    private LocalDateTime createdAt;
}
