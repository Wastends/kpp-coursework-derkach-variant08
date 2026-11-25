package ua.edu.nupp.kpp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class MeasurementCsv {

    private static final String HEADER = "sample_id;parameter_name;unit;measured_value;limit_value";

    private MeasurementCsv() {
    }

    static Measurement readFirst(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !HEADER.equals(header.replaceFirst("^\uFEFF", "").strip())) {
                throw new IOException("Невідомий заголовок CSV. Використайте формат measurements.csv.");
            }

            String row;
            int line = 1;
            while ((row = reader.readLine()) != null) {
                line++;
                if (!row.isBlank()) {
                    return parse(row, line);
                }
            }
            throw new IOException("У CSV немає рядків вимірювань.");
        }
    }

    private static Measurement parse(String row, int line) throws IOException {
        String[] columns = row.split(";", -1);
        if (columns.length != 5) {
            throw new IOException("Рядок " + line + " має містити п’ять полів, розділених «;».");
        }
        try {
            long sampleId = Long.parseLong(columns[0].strip());
            String parameter = columns[1].strip();
            String unit = columns[2].strip();
            if (sampleId <= 0 || parameter.isEmpty() || unit.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return new Measurement(0, sampleId, parameter, unit, new BigDecimal(columns[3].strip()),
                    new BigDecimal(columns[4].strip()));
        } catch (IllegalArgumentException error) {
            throw new IOException("Рядок " + line + " містить некоректні дані вимірювання.", error);
        }
    }
}
