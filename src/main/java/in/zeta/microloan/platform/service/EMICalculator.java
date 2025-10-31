package in.zeta.microloan.platform.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EMICalculator {

    /**
     * Calculate EMI using the formula:
     * EMI = P × r × (1 + r)^n / [(1 + r)^n - 1]
     * where:
     * P = Principal loan amount
     * r = Monthly interest rate (annual rate / 12 / 100)
     * n = Loan tenure in months
     */
    public static BigDecimal calculateEMI(BigDecimal principal,
                                          BigDecimal annualInterestRate,
                                          int tenureMonths) {

        // Convert annual interest rate to monthly rate
        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // If interest rate is 0, return simple division
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(
                    BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        // Calculate (1 + r)^n
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(tenureMonths);

        // Calculate numerator: P × r × (1 + r)^n
        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(onePlusRatePowerN);

        // Calculate denominator: (1 + r)^n - 1
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);

        // Calculate EMI
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total interest payable
     */
    public static BigDecimal calculateTotalInterest(BigDecimal emiAmount,
                                                    int tenureMonths,
                                                    BigDecimal principal) {
        BigDecimal totalPayment = emiAmount.multiply(BigDecimal.valueOf(tenureMonths));
        return totalPayment.subtract(principal);
    }

    /**
     * Calculate total amount payable
     */
    public static BigDecimal calculateTotalPayable(BigDecimal principal,
                                                   BigDecimal totalInterest) {
        return principal.add(totalInterest);
    }
}
