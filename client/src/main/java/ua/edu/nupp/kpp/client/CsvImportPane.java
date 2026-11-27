package ua.edu.nupp.kpp.client;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

final class CsvImportPane extends VBox {

    private static final Logger LOG = Logger.getLogger(CsvImportPane.class.getName());
    private final CrudPane<Measurement> pane;
    private final HttpWaterQualityClient api;
    private final ExecutorService executor;
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("CSV не імпортовано");
    private final Button cancel = new Button("Скасувати");

    CsvImportPane(CrudPane<Measurement> pane, HttpWaterQualityClient api, ExecutorService executor) {
        super(8);
        this.pane = pane;
        this.api = api;
        this.executor = executor;
        Button start = new Button("Імпортувати CSV");
        pane.disableWhileBusy(start);
        start.setOnAction(event -> chooseFile());
        cancel.setDisable(true);
        progress.setMaxWidth(Double.MAX_VALUE);
        status.setWrapText(true);
        getChildren().addAll(new FlowPane(8, 8, start, cancel), progress, status);
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Виберіть файл вимірювань");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файли CSV", "*.csv"));
        Path directory = Path.of("sample-data");
        if (!Files.isDirectory(directory)) {
            directory = Path.of("client", "sample-data");
        }
        if (Files.isDirectory(directory)) {
            chooser.setInitialDirectory(directory.toFile());
        }
        File file = chooser.showOpenDialog(pane.getScene().getWindow());
        if (file != null) {
            startImport(file.toPath());
        }
    }

    void startImport(Path path) {
        if (!pane.beginExternalTask()) {
            return;
        }
        ImportMeasurementsTask task = new ImportMeasurementsTask(path, api);
        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());
        cancel.setDisable(false);
        cancel.setOnAction(event -> {
            task.requestStop();
            cancel.setDisable(true);
        });
        task.setOnSucceeded(event -> finish("Імпорт завершено. " + task.summary(), null, 1));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String reason = error.getMessage() == null ? "Помилка виконання імпорту." : error.getMessage();
            finish("Імпорт припинено. " + task.summary(), reason, Math.max(0, task.getProgress()));
        });
        task.setOnCancelled(event -> finish(
                "Імпорт скасовано. Збережені записи залишаються в БД. " + task.summary(), null,
                Math.max(0, task.getProgress())));
        executor.execute(task);
    }

    private void finish(String message, String error, double finalProgress) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        progress.setProgress(finalProgress);
        status.setText(error == null ? message : message + " " + error);
        cancel.setDisable(true);
        cancel.setOnAction(null);
        pane.endExternalTask();
        if (error != null) {
            LOG.warning(error);
            pane.showError(error, true);
        }
        pane.refresh(0, message + " ", false);
    }
}
