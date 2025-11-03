package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.ContactMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionActivity {
    private UUID id;
    private UUID loanId;
    private String activityType; // reminder, follow_up, escalation, legal
    private ContactMethod contactMethod;
    private String borrowerResponse;
    private LocalDate promiseToPayDate;
    private String paymentArrangement;
    private String notes;
    private UUID assignedTo;
    private LocalDateTime activityDate;
    private LocalDate nextFollowUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
