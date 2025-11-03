package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {
    public LoanResponseDTO toResponse(Loan l) {
        return LoanResponseDTO.builder()
                .id(l.getId())
                .loanNumber(l.getLoanNumber())
                .borrowerId(l.getBorrowerId())
                .householdId(l.getHouseholdId())
                .principalAmount(l.getPrincipalAmount())
                .interestRate(l.getInterestRate())
                .tenureMonths(l.getTenureMonths())
                .emiAmount(l.getEmiAmount())
                .totalPayable(l.getTotalPayable())
                .totalOutstanding(l.getTotalOutstanding())
                .totalPaid(l.getTotalPaid())
                .disbursementDate(l.getDisbursementDate())
                .firstDueDate(l.getFirstDueDate())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .build();
    }

    public LoanDetailResponseDTO toDetail(Loan l, Borrower b) {
        return LoanDetailResponseDTO.builder()
                .id(l.getId())
                .loanNumber(l.getLoanNumber())
                .borrowerId(b.getId())
                .borrowerName(b.getName())
                .borrowerPhone(b.getPhone())
                .principalAmount(l.getPrincipalAmount())
                .interestRate(l.getInterestRate())
                .tenureMonths(l.getTenureMonths())
                .emiAmount(l.getEmiAmount())
                .totalPayable(l.getTotalPayable())
                .outstandingPrincipal(l.getOutstandingPrincipal())
                .outstandingInterest(l.getOutstandingInterest())
                .totalOutstanding(l.getTotalOutstanding())
                .totalPaid(l.getTotalPaid())
                .disbursementDate(l.getDisbursementDate())
                .firstDueDate(l.getFirstDueDate())
                .lastPaymentDate(l.getLastPaymentDate())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .build();
    }
}