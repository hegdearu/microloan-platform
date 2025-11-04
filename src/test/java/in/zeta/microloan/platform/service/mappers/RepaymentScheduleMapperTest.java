package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.model.enums.InstallmentStatus;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepaymentScheduleMapperTest {

    private RepaymentScheduleMapper mapper;
    private RepaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        mapper = new RepaymentScheduleMapper();

        schedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .installmentNumber(1)
                .dueDate(LocalDate.now().plusMonths(1))
                .principalDue(new BigDecimal("8000"))
                .interestDue(new BigDecimal("2000"))
                .totalDue(new BigDecimal("10000"))
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .lateFeePaid(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .status(InstallmentStatus.PENDING)
                .paidDate(null)
                .build();
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        RepaymentScheduleResponseDTO result = mapper.toResponse(schedule);

        assertNotNull(result);
        assertEquals(schedule.getId(), result.getId());
        assertEquals(schedule.getInstallmentNumber(), result.getInstallmentNumber());
        assertEquals(schedule.getDueDate(), result.getDueDate());
        assertEquals(schedule.getPrincipalDue(), result.getPrincipalDue());
        assertEquals(schedule.getInterestDue(), result.getInterestDue());
        assertEquals(schedule.getTotalDue(), result.getTotalDue());
        assertEquals(schedule.getPrincipalPaid(), result.getPrincipalPaid());
        assertEquals(schedule.getInterestPaid(), result.getInterestPaid());
        assertEquals(schedule.getLateFeePaid(), result.getLateFeePaid());
        assertEquals(schedule.getTotalPaid(), result.getTotalPaid());
        assertEquals(schedule.getStatus().name(), result.getStatus());
        assertEquals(schedule.getPaidDate(), result.getPaidDate());
    }

    @Test
    void toResponse_WithPaidSchedule_ShouldIncludePaidDate() {
        schedule.setStatus(InstallmentStatus.PAID);
        schedule.setPaidDate(LocalDate.now());
        schedule.setPrincipalPaid(schedule.getPrincipalDue());
        schedule.setInterestPaid(schedule.getInterestDue());
        schedule.setTotalPaid(schedule.getTotalDue());

        RepaymentScheduleResponseDTO result = mapper.toResponse(schedule);

        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        assertNotNull(result.getPaidDate());
        assertEquals(schedule.getTotalDue(), result.getTotalPaid());
    }
}
