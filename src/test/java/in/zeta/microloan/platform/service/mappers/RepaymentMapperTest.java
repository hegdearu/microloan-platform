package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.model.Repayment;
import in.zeta.microloan.platform.model.enums.PaymentMethod;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepaymentMapperTest {

    private RepaymentMapper mapper;
    private Repayment repayment;

    @BeforeEach
    void setUp() {
        mapper = new RepaymentMapper();

        repayment = Repayment.builder()
                .id(UUID.randomUUID())
                .receiptNumber("RCP-20240101-123456")
                .loanId(UUID.randomUUID())
                .amount(new BigDecimal("10000"))
                .principalPaid(new BigDecimal("8000"))
                .interestPaid(new BigDecimal("2000"))
                .lateFeePaid(BigDecimal.ZERO)
                .advancePayment(BigDecimal.ZERO)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .transactionRef("TXN123456")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        String message = "Payment successful";
        RepaymentResponseDTO result = mapper.toResponse(repayment, message);

        assertNotNull(result);
        assertEquals(repayment.getId(), result.getId());
        assertEquals(repayment.getReceiptNumber(), result.getReceiptNumber());
        assertEquals(repayment.getLoanId(), result.getLoanId());
        assertEquals(repayment.getAmount(), result.getAmount());
        assertEquals(repayment.getPrincipalPaid(), result.getPrincipalPaid());
        assertEquals(repayment.getInterestPaid(), result.getInterestPaid());
        assertEquals(repayment.getLateFeePaid(), result.getLateFeePaid());
        assertEquals(repayment.getAdvancePayment(), result.getAdvancePayment());
        assertEquals(repayment.getPaymentDate(), result.getPaymentDate());
        assertEquals(repayment.getPaymentMethod().name(), result.getPaymentMethod());
        assertEquals(repayment.getTransactionRef(), result.getTransactionRef());
        assertEquals(repayment.getStatus().name(), result.getStatus());
        assertEquals(repayment.getCreatedAt(), result.getCreatedAt());
        assertEquals(message, result.getMessage());
    }

    @Test
    void toResponse_WithNullMessage_ShouldMapWithoutMessage() {
        RepaymentResponseDTO result = mapper.toResponse(repayment, null);

        assertNotNull(result);
        assertNull(result.getMessage());
    }
}
