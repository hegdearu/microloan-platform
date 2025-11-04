package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.model.Repayment;
import org.springframework.stereotype.Component;

@Component
public class RepaymentMapper {
    public RepaymentResponseDTO toResponse(Repayment r, String message) {
        return RepaymentResponseDTO.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .loanId(r.getLoanId())
                .amount(r.getAmount())
                .principalPaid(r.getPrincipalPaid())
                .interestPaid(r.getInterestPaid())
                .lateFeePaid(r.getLateFeePaid())
                .advancePayment(r.getAdvancePayment())
                .paymentDate(r.getPaymentDate())
                .paymentMethod(r.getPaymentMethod().name())
                .transactionRef(r.getTransactionRef())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .message(message)
                .build();
    }
}