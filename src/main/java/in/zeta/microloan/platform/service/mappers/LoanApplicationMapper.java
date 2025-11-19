package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.model.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanApplicationMapper {
    public LoanApplicationResponseDTO toResponse(LoanApplication a) {
        return LoanApplicationResponseDTO.builder()
                .id(a.getId())
                .applicationNumber(a.getApplicationNumber())
                .borrowerId(a.getBorrowerId())
                .productId(a.getProductId())
                .requestedAmount(a.getRequestedAmount())
                .purpose(a.getPurpose())
                .preferredTenure(a.getPreferredTenure())
                .status(a.getStatus().name())
                .approvedAmount(a.getApprovedAmount())
                .approvedAt(a.getApprovedAt())
                .rejectionReason(a.getRejectionReason())
                .expiresAt(a.getExpiresAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}