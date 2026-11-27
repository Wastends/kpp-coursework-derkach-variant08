package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    @Test
    void blankPointCodeRejectedBeforeDatabaseAccess() {
        var point = new ObservationPoint(0, " ", "Навчальний", "Полтава", "Питне", 50);
        var error = assertThrows(IllegalArgumentException.class, () -> new ObservationPointService().add(point));
        assertTrue(error.getMessage().contains("код пункту"));
    }

    @Test
    void invalidSensitivityRejectedBeforeDatabaseAccess() {
        var point = new ObservationPoint(0, "PT-X", "Навчальний", "Полтава", "Питне", 20);
        assertThrows(IllegalArgumentException.class, () -> new ObservationPointService().add(point));
    }

    @Test
    void negativeMeasurementRejectedBeforeDatabaseAccess() {
        var measurement = new Measurement(0, 1, "Нітрати", "мг/л", new BigDecimal("-1"), BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> new MeasurementService().add(measurement));
    }

    @Test
    void zeroLimitRejectedBeforeDatabaseAccess() {
        var measurement = new Measurement(0, 1, "Нітрати", "мг/л", BigDecimal.ONE, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new MeasurementService().add(measurement));
    }

    @Test
    void excessiveDecimalPlacesRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceValidation.requireDecimal(new BigDecimal("0.00001"), "значення"));
    }

    @Test
    void exactDecimalBoundaryAccepted() {
        assertDoesNotThrow(() -> ServiceValidation.requireDecimal(new BigDecimal("99999999.9999"), "значення"));
    }

    @Test
    void fractionalSecondRejectedBeforeDatabaseAccess() {
        var sample = new WaterSample(0, 1, "S-X", LocalDateTime.of(2026, 1, 1, 12, 0, 0, 1), null);
        assertThrows(IllegalArgumentException.class, () -> new WaterSampleService().add(sample));
    }

    @Test
    void absentCheckResultRejectedBeforeDatabaseAccess() {
        var check = new SampleCheck(0, 1, LocalDateTime.of(2026, 1, 1, 12, 0), null, "Перевірка");
        assertThrows(IllegalArgumentException.class, () -> new SampleCheckService().add(check));
    }
}
