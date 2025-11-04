package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.model.CollectionActivity;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.OverdueTracking;
import org.springframework.stereotype.Component;

@Component
public class CollectionMapper {

    public CollectionActivityResponseDTO toActivityResponse(CollectionActivity activity) {
        return CollectionActivityResponseDTO.builder()
                .id(activity.getId())
                .loanId(activity.getLoanId())
                .activityType(activity.getActivityType())
                .contactMethod(activity.getContactMethod())
                .borrowerResponse(activity.getBorrowerResponse())
                .promiseToPayDate(activity.getPromiseToPayDate())
                .notes(activity.getNotes())
                .activityDate(activity.getActivityDate())
                .nextFollowUpDate(activity.getNextFollowUpDate())
                .build();
    }

    public OverdueLoansResponseDTO toOverdueResponse(
            OverdueTracking overdue,
            Loan loan,
            Borrower borrower) {

        return OverdueLoansResponseDTO.builder()
                .loanId(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .borrowerId(borrower.getId())
                .borrowerName(borrower.getName())
                .borrowerPhone(borrower.getPhone())
                .overdueSince(overdue.getOverdueSince())
                .overdueDays(overdue.getOverdueDays())
                .overdueAmount(overdue.getOverdueAmount())
                .penaltyAmount(overdue.getPenaltyAmount())
                .totalDue(overdue.getTotalDue())
                .collectionStage(overdue.getCollectionStage())
                .build();
    }
}
