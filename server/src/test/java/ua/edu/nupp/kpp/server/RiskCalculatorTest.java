package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskCalculatorTest {

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static Measurement measurement(String value, String limit) {
        return new Measurement(0, 1, "Нітрати", "мг/л", decimal(value), decimal(limit));
    }

    private static void equalNumber(String expected, BigDecimal actual) {
        assertEquals(0, decimal(expected).compareTo(actual));
    }

    @Test
    void doesNotCountEqualityAsExceedance() {
        RiskAssessment assessment = RiskCalculator.assess(List.of(measurement("10", "10")), 50);
        equalNumber("0", assessment.f1());
        equalNumber("0", assessment.f2());
        assertEquals(0, assessment.criticalParameterCount());
    }

    @Test
    void calculatesF1ForFifteenAgainstTen() {
        equalNumber("50", RiskCalculator.assess(List.of(measurement("15", "10")), 50).f1());
    }

    @Test
    void usesMaximumAndShareOfCriticalMeasurements() {
        RiskAssessment result = RiskCalculator.assess(List.of(
                measurement("15", "10"), measurement("0.2", "0.4")), 50);
        equalNumber("50", result.f1());
        equalNumber("50", result.f2());
        assertEquals(1, result.criticalParameterCount());
        assertEquals(2, result.measuredParameterCount());
        assertEquals(50, result.f3());
        assertEquals(decimal("50.00"), result.index());
    }

    @Test
    void capsExceedanceAtOneHundred() {
        equalNumber("100", RiskCalculator.assess(List.of(measurement("31", "10")), 50).f1());
    }

    @Test
    void appliesTheAgreedWeights() {
        assertEquals(decimal("72.75"), RiskCalculator.calculate(decimal("80"), decimal("45"), decimal("100")));
    }

    @Test
    void acceptsFactorBoundaries() {
        assertEquals(decimal("0.00"), RiskCalculator.calculate(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(decimal("100.00"), RiskCalculator.calculate(decimal("100"), decimal("100"), decimal("100")));
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        assertEquals(decimal("13.33"), RiskCalculator.calculate(decimal("33.333333333333"), BigDecimal.ZERO,
                BigDecimal.ZERO));
    }

    @Test
    void rejectsFactorBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> RiskCalculator.calculate(decimal("-1"), decimal("50"), decimal("50")));
    }

    @Test
    void rejectsFactorAboveOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> RiskCalculator.calculate(decimal("50"), decimal("101"), decimal("50")));
    }

    @Test
    void rejectsEmptyMeasurements() {
        assertThrows(IllegalArgumentException.class, () -> RiskCalculator.assess(List.of(), 50));
    }

    @Test
    void rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> RiskCalculator.assess(List.of(measurement("1", "0")), 50));
    }

    @Test
    void rejectsNegativeMeasurement() {
        assertThrows(IllegalArgumentException.class,
                () -> RiskCalculator.assess(List.of(measurement("-1", "10")), 50));
    }
}
