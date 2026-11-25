package ua.edu.nupp.kpp.server;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class RiskCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal F1_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal F2_WEIGHT = new BigDecimal("0.35");
    private static final BigDecimal F3_WEIGHT = new BigDecimal("0.25");
    private static final int FACTOR_SCALE = 12;

    private RiskCalculator() {
    }

    public static RiskAssessment assess(List<Measurement> measurements, int sensitivityScore) {
        if (measurements == null || measurements.isEmpty()) {
            throw new IllegalArgumentException("Для розрахунку потрібне хоча б одне вимірювання.");
        }

        BigDecimal maxExceedance = BigDecimal.ZERO;
        long criticalCount = 0;
        for (Measurement measurement : measurements) {
            BigDecimal value = measurement.getMeasuredValue();
            BigDecimal limit = measurement.getLimitValue();
            if (value == null || value.signum() < 0 || limit == null || limit.signum() <= 0) {
                throw new IllegalArgumentException("Некоректне виміряне значення або контрольна межа.");
            }
            if (value.compareTo(limit) > 0) {
                criticalCount++;
                maxExceedance = maxExceedance.max(toPercent(value.subtract(limit), limit).min(HUNDRED));
            }
        }

        BigDecimal f2 = toPercent(BigDecimal.valueOf(criticalCount), BigDecimal.valueOf(measurements.size()));
        BigDecimal index = calculate(maxExceedance, f2, BigDecimal.valueOf(sensitivityScore));
        return new RiskAssessment(maxExceedance, f2, sensitivityScore, criticalCount, measurements.size(), index);
    }

    public static BigDecimal calculate(BigDecimal f1, BigDecimal f2, BigDecimal f3) {
        requireFactor(f1);
        requireFactor(f2);
        requireFactor(f3);
        return f1.multiply(F1_WEIGHT)
                .add(f2.multiply(F2_WEIGHT))
                .add(f3.multiply(F3_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toPercent(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, FACTOR_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static void requireFactor(BigDecimal factor) {
        if (factor == null || factor.signum() < 0 || factor.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Фактор оцінки має належати діапазону від 0 до 100.");
        }
    }
}
