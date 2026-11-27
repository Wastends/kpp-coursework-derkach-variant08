package ua.edu.nupp.kpp.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementCsvTest {

    private static final String HEADER = "sample_id;parameter_name;unit;measured_value;limit_value";
    @TempDir Path folder;

    private Path file(String contents) throws IOException {
        return Files.writeString(folder.resolve("sample.csv"), contents, StandardCharsets.UTF_8);
    }

    @Test void validRowsPreserveUnicodeAndDecimalPrecision() throws IOException {
        var rows = MeasurementCsv.readRows(file(HEADER + "\n2;Фосфати;мг/л;0.3500;0.4000\n2;Амоній;мг/л;0.6500;0.5000"));
        assertEquals(2, rows.size());
        var value = MeasurementCsv.parse(rows.getFirst());
        assertEquals(2, value.getSampleId());
        assertEquals("Фосфати", value.getParameterName());
        assertEquals(new BigDecimal("0.3500"), value.getMeasuredValue());
    }

    @Test void blankRowsDoNotChangePhysicalLineNumbers() throws IOException {
        var rows = MeasurementCsv.readRows(file(HEADER + "\n\n2;Фосфати;мг/л;0.35;0.40"));
        assertEquals(1, rows.size());
        assertEquals(3, rows.getFirst().number());
    }

    @Test void headerOnlyHasNoRows() throws IOException {
        assertTrue(MeasurementCsv.readRows(file(HEADER)).isEmpty());
    }

    @Test void acceptsUtf8Bom() throws IOException {
        assertTrue(MeasurementCsv.readRows(file("\uFEFF" + HEADER)).isEmpty());
    }

    @Test void rejectsWrongHeaderOrderBeforeParsing() throws IOException {
        Path path = file(HEADER.replace("measured_value;limit_value", "limit_value;measured_value"));
        assertThrows(IOException.class, () -> MeasurementCsv.readRows(path));
    }

    @Test void rejectsMissingFile() {
        assertThrows(IOException.class, () -> MeasurementCsv.readRows(folder.resolve("absent.csv")));
    }

    @Test void rejectsIncompleteRow() {
        assertThrows(IOException.class,
                () -> MeasurementCsv.parse(new MeasurementCsv.Row(2, "2;Фосфати;мг/л;0.35")));
    }

    @Test void rejectsInvalidNumber() {
        assertThrows(IOException.class,
                () -> MeasurementCsv.parse(new MeasurementCsv.Row(2, "2;Фосфати;мг/л;немає;0.40")));
    }

    @Test void rejectsEmptyRequiredText() {
        assertThrows(IOException.class,
                () -> MeasurementCsv.parse(new MeasurementCsv.Row(2, "2;;мг/л;0.35;0.40")));
    }
}
