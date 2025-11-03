package in.zeta.microloan.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CollectionActivityRequestDTO {

    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @NotBlank(message = "Activity type is required")
    private String activityType;

    @NotBlank(message = "Contact method is required")
    private String contactMethod;

    private String borrowerResponse;
    private LocalDate promiseToPayDate;
    private String paymentArrangement;

    @NotBlank(message = "Notes are required")
    @Size(min = 20, message = "Notes must be at least 20 characters")
    private String notes;

    @NotNull(message = "Assigned agent is required")
    private Long assignedTo;

    private LocalDate nextFollowUpDate;
}
