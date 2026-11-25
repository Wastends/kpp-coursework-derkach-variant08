package ua.edu.nupp.kpp.server;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

final class ServiceValidation {

    private static final BigDecimal MAX_MEASUREMENT = new BigDecimal("99999999.9999");

    private ServiceValidation() {
    }

    static void requireId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Ідентифікатор має бути додатним.");
        }
    }

    static void required(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Заповніть поле «" + field + "».");
        }
    }

    static void requireText(String text, int maxLength, String field) {
        if (text == null || text.isBlank() || text.codePointCount(0, text.length()) > maxLength) {
            throw new IllegalArgumentException("Поле «" + field + "» має містити від 1 до " + maxLength + " символів.");
        }
    }

    static void requireDateTime(LocalDateTime value, String field) {
        required(value, field);
        if (value.getYear() < 1000 || value.getYear() > 9999 || value.getNano() != 0) {
            throw new IllegalArgumentException("Поле «" + field
                    + "» має містити дату в межах 1000–9999 років і час із точністю до секунди.");
        }
    }

    static void requireDecimal(BigDecimal value, String field) {
        required(value, field);
        if (value.abs().compareTo(MAX_MEASUREMENT) > 0 || value.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("Поле «" + field
                    + "» має містити число з максимум 8 цифрами до коми та 4 після неї.");
        }
    }

    static <T> T found(Optional<T> row, long id) {
        return row.orElseThrow(() -> notFound(id));
    }

    static void requireChanged(boolean changed, long id) {
        if (!changed) {
            throw notFound(id);
        }
    }

    private static NoSuchElementException notFound(long id) {
        return new NoSuchElementException("Запис із ID " + id + " не знайдено.");
    }
}
