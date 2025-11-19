package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import org.springframework.stereotype.Component;

@Component
public class RepaymentScheduleMapper {
    public RepaymentScheduleResponseDTO toResponse(RepaymentSchedule s) {
        return RepaymentScheduleResponseDTO.builder()
                .id(s.getId())
                .installmentNumber(s.getInstallmentNumber())
                .dueDate(s.getDueDate())
                .principalDue(s.getPrincipalDue())
                .interestDue(s.getInterestDue())
                .totalDue(s.getTotalDue())
                .principalPaid(s.getPrincipalPaid())
                .interestPaid(s.getInterestPaid())
                .lateFeePaid(s.getLateFeePaid())
                .totalPaid(s.getTotalPaid())
                .status(s.getStatus().name())
                .paidDate(s.getPaidDate())
                .build();
    }
}
