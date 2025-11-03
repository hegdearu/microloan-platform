package in.zeta.microloan.platform.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class EMICalculatorTest {

    @Test
    void testCalculateEMI_WithInterest() {
        // Arrange
        BigDecimal principal = new BigDecimal("100000");
        BigDecimal annualInterestRate = new BigDecimal("12");
        int tenureMonths = 12;

        // Act
        BigDecimal emi = EMICalculator.calculateEMI(principal, annualInterestRate, tenureMonths);

        // Assert
        assertNotNull(emi);
        assertTrue(emi.compareTo(BigDecimal.ZERO) > 0);
        // EMI should be around 8884.88
        assertTrue(emi.compareTo(new BigDecimal("8800")) > 0);
        assertTrue(emi.compareTo(new BigDecimal("8900")) < 0);
    }

    @Test
    void testCalculateEMI_ZeroInterest() {
        // Arrange
        BigDecimal principal = new BigDecimal("100000");
        BigDecimal annualInterestRate = BigDecimal.ZERO;
        int tenureMonths = 10;

        // Act
        BigDecimal emi = EMICalculator.calculateEMI(principal, annualInterestRate, tenureMonths);

        // Assert
        assertNotNull(emi);
        assertEquals(new BigDecimal("10000.00"), emi);
    }

    @Test
    void testCalculateEMI_LargePrincipal() {
        // Arrange
        BigDecimal principal = new BigDecimal("1000000");
        BigDecimal annualInterestRate = new BigDecimal("10.5");
        int tenureMonths = 24;

        // Act
        BigDecimal emi = EMICalculator.calculateEMI(principal, annualInterestRate, tenureMonths);

        // Assert
        assertNotNull(emi);
        assertTrue(emi.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(emi.compareTo(new BigDecimal("45000")) > 0);
        assertTrue(emi.compareTo(new BigDecimal("48000")) < 0);
    }

    @Test
    void testCalculateTotalInterest() {
        // Arrange
        BigDecimal emiAmount = new BigDecimal("8884.88");
        int tenureMonths = 12;
        BigDecimal principal = new BigDecimal("100000");

        // Act
        BigDecimal totalInterest = EMICalculator.calculateTotalInterest(emiAmount, tenureMonths, principal);

        // Assert
        assertNotNull(totalInterest);
        assertTrue(totalInterest.compareTo(BigDecimal.ZERO) > 0);
        // Total interest should be around 6618.56
        assertTrue(totalInterest.compareTo(new BigDecimal("6000")) > 0);
        assertTrue(totalInterest.compareTo(new BigDecimal("7000")) < 0);
    }

    @Test
    void testCalculateTotalPayable() {
        // Arrange
        BigDecimal principal = new BigDecimal("100000");
        BigDecimal totalInterest = new BigDecimal("6618.56");

        // Act
        BigDecimal totalPayable = EMICalculator.calculateTotalPayable(principal, totalInterest);

        // Assert
        assertNotNull(totalPayable);
        assertEquals(new BigDecimal("106618.56"), totalPayable);
    }

    @Test
    void testCalculateTotalPayable_ZeroInterest() {
        // Arrange
        BigDecimal principal = new BigDecimal("50000");
        BigDecimal totalInterest = BigDecimal.ZERO;

        // Act
        BigDecimal totalPayable = EMICalculator.calculateTotalPayable(principal, totalInterest);

        // Assert
        assertNotNull(totalPayable);
        assertEquals(new BigDecimal("50000"), totalPayable);
    }
}