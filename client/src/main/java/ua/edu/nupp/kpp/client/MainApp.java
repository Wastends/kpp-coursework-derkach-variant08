package ua.edu.nupp.kpp.client;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static java.util.List.of;

public final class MainApp extends Application {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void start(Stage stage) {
        String url = System.getenv().getOrDefault("KPP_API_URL", HttpWaterQualityClient.DEFAULT_URL);
        HttpWaterQualityClient api = new HttpWaterQualityClient(url);
        CrudPane<ObservationPoint> points = points(api);
        CrudPane<WaterSample> samples = samples(api);
        CrudPane<Measurement> measurements = measurements(api);
        CrudPane<SampleCheck> checks = checks(api);

        Label heading = new Label("Моніторинг якості води");
        heading.getStyleClass().add("app-title");
        Label server = new Label(url);
        server.getStyleClass().add("server-address");
        VBox header = new VBox(5, heading, server);
        header.getStyleClass().add("app-header");

        TabPane tabs = new TabPane(new Tab("Пункти спостереження", points), new Tab("Проби", samples),
                new Tab("Вимірювання", measurements), new Tab("Перевірки", checks));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        BorderPane root = new BorderPane(tabs);
        root.setTop(header);
        Scene scene = new Scene(root, 1260, 720);
        scene.getStylesheets().add(MainApp.class.getResource("app.css").toExternalForm());
        stage.setTitle(heading.getText());
        stage.setMinWidth(1000);
        stage.setMinHeight(570);
        stage.setScene(scene);
        stage.show();
        for (CrudPane<?> pane : List.of(points, samples, measurements, checks)) {
            pane.refresh(0, "", false);
        }
    }

    private CrudPane<ObservationPoint> points(HttpWaterQualityClient api) {
        TableView<ObservationPoint> table = new TableView<>();
        table.getColumns().setAll(of(column("ID", ObservationPoint::getId, 55),
                column("Код", ObservationPoint::getCode, 85), column("Назва", ObservationPoint::getName, 145),
                column("Місце", ObservationPoint::getLocation, 145),
                column("Водокористування", ObservationPoint::getWaterUse, 140),
                column("Чутливість", ObservationPoint::getSensitivityScore, 90)));
        var form = new EntryForm<ObservationPoint>(
                of(field("Код", "PT-03"), field("Назва", ""), field("Місце", ""),
                        field("Водокористування", ""), field("Чутливість", "25, 50, 75 або 100")),
                p -> of(p.getCode(), p.getName(), p.getLocation(), p.getWaterUse(), "" + p.getSensitivityScore()),
                (id, f) -> new ObservationPoint(id, f.text(0), f.text(1), f.text(2), f.text(3), f.integer(4)));
        return new CrudPane<>(api.points, table, form, ObservationPoint::getId, executor);
    }

    private CrudPane<WaterSample> samples(HttpWaterQualityClient api) {
        TableView<WaterSample> table = new TableView<>();
        table.getColumns().setAll(of(column("ID", WaterSample::getId, 60),
                column("ID пункту", WaterSample::getPointId, 85),
                column("Код проби", WaterSample::getCode, 120),
                column("Час відбору", WaterSample::getSampledAt, 150),
                column("Стан", s -> label(s.getState()), 150)));
        var form = new EntryForm<WaterSample>(of(field("ID пункту", "1"), field("Код проби", "S-003"),
                field("Час відбору", "2026-11-25T12:00:00")),
                s -> of("" + s.getPointId(), s.getCode(), s.getSampledAt().toString()),
                (id, f) -> new WaterSample(id, f.id(0), f.text(1), f.dateTime(2), null));
        var pane = new CrudPane<>(api.samples, table, form, WaterSample::getId, executor);
        ComboBox<SampleState> desired = new ComboBox<>(FXCollections.observableArrayList(SampleState.values()));
        desired.setPromptText("Оберіть стан");
        desired.setConverter(new StringConverter<>() {
            @Override public String toString(SampleState state) { return state == null ? "" : label(state); }
            @Override public SampleState fromString(String text) { throw new UnsupportedOperationException(); }
        });
        Button transition = new Button("Змінити стан");
        Button risk = new Button("Розрахувати I");
        pane.disableWithoutSelection(desired);
        pane.disableWithoutSelection(transition);
        pane.disableWithoutSelection(risk);
        Label factors = new Label();
        factors.setWrapText(true);
        factors.getStyleClass().add("risk-value");
        pane.onSelection(value -> {
            desired.getSelectionModel().clearSelection();
            factors.setText("");
        });
        transition.setOnAction(event -> {
            long id = pane.selectionId();
            SampleState target = desired.getValue();
            if (target == null) {
                pane.showError("Оберіть новий стан проби.", true);
                return;
            }
            pane.run(() -> {
                api.transition(id, target);
                return null;
            }, ignored -> pane.refresh(id, "Стан змінено. ", false), true, "");
        });
        risk.setOnAction(event -> {
            long id = pane.selectionId();
            pane.run(() -> api.risk(id), r -> {
                factors.setText("I = " + r.index().toPlainString() + "   ·   Проба " + id
                        + "\nF1 = " + r.f1().toPlainString()
                        + "   F2 = " + r.f2().toPlainString() + "   F3 = " + r.f3()
                        + "\nКритичних параметрів " + r.criticalParameterCount() + " з " + r.measuredParameterCount());
                pane.showStatus("Показник розраховано.");
            }, true, "");
        });
        pane.extra(new Separator());
        pane.extra(new FlowPane(8, 8, desired, transition, risk));
        pane.extra(factors);
        return pane;
    }

    private CrudPane<Measurement> measurements(HttpWaterQualityClient api) {
        TableView<Measurement> table = new TableView<>();
        table.getColumns().setAll(of(column("ID", Measurement::getId, 58), column("ID проби", Measurement::getSampleId, 80),
                column("Параметр", Measurement::getParameterName, 130), column("Одиниця", Measurement::getUnit, 70),
                column("Значення", Measurement::getMeasuredValue, 100), column("Межа", Measurement::getLimitValue, 90)));
        var form = new EntryForm<Measurement>(of(field("ID проби", "1"), field("Параметр", "Нітрати"),
                field("Одиниця", "мг/л"), field("Значення", "15,00"), field("Контрольна межа", "10,00")),
                m -> of("" + m.getSampleId(), m.getParameterName(), m.getUnit(),
                        m.getMeasuredValue().toPlainString(), m.getLimitValue().toPlainString()),
                (id, f) -> new Measurement(id, f.id(0), f.text(1), f.text(2), f.decimal(3), f.decimal(4)));
        var pane = new CrudPane<>(api.measurements, table, form, Measurement::getId, executor);
        pane.extra(new Separator());
        pane.extra(new CsvImportPane(pane, api, executor));
        return pane;
    }

    private CrudPane<SampleCheck> checks(HttpWaterQualityClient api) {
        TableView<SampleCheck> table = new TableView<>();
        table.getColumns().setAll(of(column("ID", SampleCheck::getId, 60), column("ID проби", SampleCheck::getSampleId, 80),
                column("Час перевірки", SampleCheck::getCheckedAt, 145),
                column("Результат", c -> c.getResult() == CheckResult.PASSED ? "Успішна" : "Неуспішна", 100),
                column("Примітка", SampleCheck::getNote, 200)));
        var form = new EntryForm<SampleCheck>(of(field("ID проби", "1"), field("Час перевірки", "2026-11-25T13:00:00"),
                field("Результат", "PASSED або FAILED"), field("Примітка", "")),
                c -> of("" + c.getSampleId(), c.getCheckedAt().toString(), c.getResult().name(), c.getNote()),
                (id, f) -> new SampleCheck(id, f.id(0), f.dateTime(1), f.enumValue(2, CheckResult.class), f.text(3)));
        return new CrudPane<>(api.checks, table, form, SampleCheck::getId, executor);
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, ?> value, int width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(row -> new ReadOnlyStringWrapper(String.valueOf(value.apply(row.getValue()))));
        return column;
    }

    private static EntryForm.Field field(String name, String hint) {
        return new EntryForm.Field(name, hint);
    }

    private static String label(SampleState state) {
        return switch (state) {
            case REGISTERED -> "Зареєстровано";
            case VALIDATED -> "Валідовано";
            case ANALYZED -> "Проаналізовано";
            case ARCHIVED -> "Архівовано";
        };
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
