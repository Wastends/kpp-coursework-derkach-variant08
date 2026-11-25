package ua.edu.nupp.kpp.client;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class EntryForm<T> extends GridPane {

    @FunctionalInterface
    interface Reader<T> {
        T read(long id, EntryForm<T> fields);
    }

    record Field(String label, String hint) {
    }

    private final List<TextField> controls = new ArrayList<>();
    private final List<Field> fields;
    private final Function<T, List<String>> display;
    private final Reader<T> reader;

    EntryForm(List<Field> fields, Function<T, List<String>> display, Reader<T> reader) {
        this.fields = fields;
        this.display = display;
        this.reader = reader;
        setHgap(10);
        setVgap(10);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            TextField input = new TextField();
            input.setPromptText(field.hint());
            Label label = new Label(field.label());
            label.setLabelFor(input);
            controls.add(input);
            addRow(i, label, input);
            setHgrow(input, Priority.ALWAYS);
        }
    }

    void show(T value) {
        if (value == null) {
            controls.forEach(TextField::clear);
            return;
        }
        List<String> values = display.apply(value);
        for (int i = 0; i < controls.size(); i++) {
            controls.get(i).setText(values.get(i));
        }
    }

    T value(long id) {
        return reader.read(id, this);
    }

    String text(int i) {
        String value = controls.get(i).getText().strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Заповніть поле «" + fields.get(i).label() + "».");
        }
        return value;
    }

    long id(int i) {
        long value = parsed(i, Long::parseLong);
        if (value <= 0) {
            throw invalid(i);
        }
        return value;
    }

    int integer(int i) {
        return parsed(i, Integer::parseInt);
    }

    BigDecimal decimal(int i) {
        return parsed(i, value -> new BigDecimal(value.replace(',', '.')));
    }

    LocalDateTime dateTime(int i) {
        return parsed(i, LocalDateTime::parse);
    }

    <E extends Enum<E>> E enumValue(int i, Class<E> type) {
        return parsed(i, value -> Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT)));
    }

    private <V> V parsed(int i, Function<String, V> parser) {
        String value = text(i);
        try {
            return parser.apply(value);
        } catch (IllegalArgumentException | DateTimeParseException error) {
            throw invalid(i);
        }
    }

    private IllegalArgumentException invalid(int i) {
        return new IllegalArgumentException("Некоректне значення поля «" + fields.get(i).label() + "».");
    }
}
