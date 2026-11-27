package ua.edu.nupp.kpp.client;

import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ImportMeasurementsTask extends Task<Integer> {

    private static final Logger LOG = Logger.getLogger(ImportMeasurementsTask.class.getName());
    private final Path path;
    private final HttpWaterQualityClient api;
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private volatile int imported;
    private volatile int processed;
    private volatile int skipped;
    private volatile int total;

    public ImportMeasurementsTask(Path path, HttpWaterQualityClient api) {
        this.path = path;
        this.api = api;
    }

    public void requestStop() {
        stopRequested.set(true);
        updateMessage("Зупинка після поточного запиту…");
    }

    public String summary() {
        return "Опрацьовано " + processed + " із " + total + ". Збережено " + imported
                + ", пропущено " + skipped + ".";
    }

    @Override
    protected Integer call() throws Exception {
        LOG.info(() -> "Початок CSV-імпорту: " + path.getFileName());
        updateMessage("Читання CSV…");
        try {
            List<MeasurementCsv.Row> rows = MeasurementCsv.readRows(path);
            total = rows.size();
            updateProgress(0, Math.max(total, 1));
            if (total == 0) {
                updateMessage("У CSV немає рядків даних.");
                updateProgress(1, 1);
            }

            for (MeasurementCsv.Row row : rows) {
                if (stopRequested.get() || isCancelled()) {
                    break;
                }
                processed++;
                Measurement measurement;
                try {
                    measurement = MeasurementCsv.parse(row);
                } catch (IOException invalid) {
                    skipped++;
                    LOG.warning(invalid.getMessage());
                    reportProgress();
                    continue;
                }
                try {
                    api.measurements.add(measurement);
                    imported++;
                } catch (IOException failure) {
                    updateProgress(processed, total);
                    LOG.log(Level.WARNING, "HTTP-помилка під час імпорту рядка " + row.number(), failure);
                    throw new IOException("Рядок " + row.number() + ": " + failure.getMessage(), failure);
                }
                reportProgress();
            }

            if (stopRequested.get() && processed < total && !isCancelled()) {
                cancel(false);
            }
            return imported;
        } catch (IOException failure) {
            LOG.warning("CSV-імпорт завершився помилкою: " + failure.getMessage());
            throw failure;
        } finally {
            LOG.info(() -> "Завершення CSV-імпорту. " + summary());
        }
    }

    private void reportProgress() {
        updateProgress(processed, total);
        updateMessage(stopRequested.get() ? "Зупинка після поточного запиту…" : summary());
    }
}
