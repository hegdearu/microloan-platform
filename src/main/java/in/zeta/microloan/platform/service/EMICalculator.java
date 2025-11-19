package in.zeta.microloan.platform.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EMICalculator {

    public static BigDecimal calculateEMI(BigDecimal principal,
                                          BigDecimal annualInterestRate,
                                          int tenureMonths) {

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal emiZeroRate = principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
            return emiZeroRate;
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(tenureMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRatePowerN);
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        return emi;
    }

    public static BigDecimal calculateTotalInterest(BigDecimal emiAmount,
                                                    int tenureMonths,
                                                    BigDecimal principal) {
        BigDecimal totalPayment = emiAmount.multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);
        return totalInterest;
    }

    public static BigDecimal calculateTotalPayable(BigDecimal principal,
                                                   BigDecimal totalInterest) {
        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable;
    }
}