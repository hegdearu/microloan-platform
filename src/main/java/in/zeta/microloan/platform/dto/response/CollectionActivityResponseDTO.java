package in.zeta.microloan.platform.dto.response;

import in.zeta.microloan.platform.model.enums.ContactMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CollectionActivityResponseDTO {
    private UUID id;
    private UUID loanId;
    private String activityType;
    private ContactMethod contactMethod;
    private String borrowerResponse;
    private LocalDate promiseToPayDate;
    private String notes;
    private LocalDateTime activityDate;
    private LocalDate nextFollowUpDate;
}
