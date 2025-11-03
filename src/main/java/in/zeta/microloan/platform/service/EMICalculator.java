package in.zeta.microloan.platform.service;

import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EMICalculator {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(EMICalculator.class);

    public static BigDecimal calculateEMI(BigDecimal principal,
                                          BigDecimal annualInterestRate,
                                          int tenureMonths) {
        spectraLogger.info("EMI_CALCULATION_REQUEST")
                .attr("principal", principal)
                .attr("annualRate", annualInterestRate)
                .attr("tenureMonths", tenureMonths)
                .log();

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal emiZeroRate = principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
            spectraLogger.info("EMI_CALCULATION_ZERO_RATE_RESULT")
                    .attr("emi", emiZeroRate)
                    .log();
            return emiZeroRate;
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(tenureMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRatePowerN);
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        spectraLogger.info("EMI_CALCULATION_RESULT")
                .attr("emi", emi)
                .log();
        return emi;
    }

    public static BigDecimal calculateTotalInterest(BigDecimal emiAmount,
                                                    int tenureMonths,
                                                    BigDecimal principal) {
        BigDecimal totalPayment = emiAmount.multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);
        spectraLogger.info("TOTAL_INTEREST_CALCULATION_RESULT")
                .attr("principal", principal)
                .attr("emi", emiAmount)
                .attr("tenureMonths", tenureMonths)
                .attr("totalInterest", totalInterest)
                .log();
        return totalInterest;
    }

    public static BigDecimal calculateTotalPayable(BigDecimal principal,
                                                   BigDecimal totalInterest) {
        BigDecimal totalPayable = principal.add(totalInterest);
        spectraLogger.info("TOTAL_PAYABLE_CALCULATION_RESULT")
                .attr("principal", principal)
                .attr("totalInterest", totalInterest)
                .attr("totalPayable", totalPayable)
                .log();
        return totalPayable;
    }
}