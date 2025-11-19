package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.model.LoanProduct;
import org.springframework.stereotype.Component;

@Component
public class LoanProductMapper {
    public LoanProductResponseDTO toResponse(LoanProduct p) {
        return LoanProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .minAmount(p.getMinAmount())
                .maxAmount(p.getMaxAmount())
                .interestRate(p.getInterestRate())
                .processingFeeType(p.getProcessingFeeType())
                .processingFeeValue(p.getProcessingFeeValue())
                .tenureMonths(p.getTenureMonths())
                .gracePeriodDays(p.getGracePeriodDays())
                .lateFeePercent(p.getLateFeePercent())
                .maxLateFeePercent(p.getMaxLateFeePercent())
                .prepaymentChargesType(p.getPrepaymentChargesType())
                .prepaymentChargesValue(p.getPrepaymentChargesValue())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
