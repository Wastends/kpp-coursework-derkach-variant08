package ua.edu.nupp.kpp.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class MeasurementCsv {

    private static final String HEADER = "sample_id;parameter_name;unit;measured_value;limit_value";

    private MeasurementCsv() {
    }

    record Row(int number, String text) {
    }

    static List<Row> readRows(Path file) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IOException("Не вдалося прочитати CSV-файл «" + file.getFileName() + "».", error);
        }
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst().replaceFirst("^\uFEFF", "").strip())) {
            throw new IOException("Невідомий заголовок CSV. Використайте формат measurements.csv.");
        }
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                rows.add(new Row(i + 1, lines.get(i)));
            }
        }
        return rows;
    }

    static Measurement parse(Row row) throws IOException {
        int line = row.number();
        String[] columns = row.text().split(";", -1);
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
