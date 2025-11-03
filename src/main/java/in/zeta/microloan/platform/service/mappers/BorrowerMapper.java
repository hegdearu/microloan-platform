package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.model.Borrower;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BorrowerMapper {

    public BorrowerResponseDTO toResponse(Borrower borrower) {
        return BorrowerResponseDTO.builder()
                .id(borrower.getId())
                .name(borrower.getName())
                .phone(borrower.getPhone())
                .email(borrower.getEmail())
                .dob(borrower.getDob())
                .householdId(borrower.getHouseholdId())
                .relationshipToHead(borrower.getRelationshipToHead())
                .isHouseholdHead(borrower.getIsHouseholdHead())
                .individualAnnualIncome(borrower.getIndividualAnnualIncome())
                .occupation(borrower.getOccupation())
                .address(borrower.getAddress())
                .idProofType(borrower.getIdProofType())
                .idProofNumber(borrower.getIdProofNumber())
                .status(borrower.getStatus())
                .isVerified(borrower.getIsVerified())
                .createdAt(borrower.getCreatedAt())
                .updatedAt(borrower.getUpdatedAt())
                .build();
    }

    public BorrowerCreditSummaryResponseDTO toCreditSummary(
            Borrower borrower,
            int totalLoans,
            int activeLoans,
            int closedLoans,
            BigDecimal totalDisbursed,
            BigDecimal totalOutstanding,
            BigDecimal totalPaid) {

        return BorrowerCreditSummaryResponseDTO.builder()
                .borrowerId(borrower.getId())
                .borrowerName(borrower.getName())
                .totalLoans(totalLoans)
                .activeLoans(activeLoans)
                .closedLoans(closedLoans)
                .totalDisbursed(totalDisbursed != null ? totalDisbursed : BigDecimal.ZERO)
                .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                .totalPaid(totalPaid != null ? totalPaid : BigDecimal.ZERO)
                .isVerified(borrower.getIsVerified())
                .status(borrower.getStatus().name())
                .build();
    }
}
