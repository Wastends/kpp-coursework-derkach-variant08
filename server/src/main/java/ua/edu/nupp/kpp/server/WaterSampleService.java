package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public final class WaterSampleService {

    private static final Logger LOG = Logger.getLogger(WaterSampleService.class.getName());

    private final WaterSampleDao dao = new WaterSampleDao();
    private final ObservationPointDao points = new ObservationPointDao();

    public long add(WaterSample value) throws SQLException {
        validate(value);
        WaterSample created = new WaterSample(0, value.getPointId(), value.getCode(), value.getSampledAt(),
                SampleState.REGISTERED);
        long id = dao.add(created);
        LOG.info(() -> "Додано пробу ID " + id + ".");
        return id;
    }

    public List<WaterSample> getAll() throws SQLException {
        return dao.getAll();
    }

    public WaterSample getById(long id) throws SQLException {
        ServiceValidation.requireId(id);
        return ServiceValidation.found(dao.getById(id), id);
    }

    public void update(WaterSample value) throws SQLException {
        validate(value);
        ServiceValidation.requireId(value.getId());
        ServiceValidation.requireChanged(dao.update(value), value.getId());
        LOG.info(() -> "Оновлено пробу ID " + value.getId() + ".");
    }

    public void delete(long id) throws SQLException {
        ServiceValidation.requireId(id);
        ServiceValidation.requireChanged(dao.delete(id), id);
        LOG.info(() -> "Видалено пробу ID " + id + ".");
    }

    public RiskAssessment calculateRisk(long sampleId) throws SQLException {
        ServiceValidation.requireId(sampleId);
        WaterSampleDao.Review data = ServiceValidation.found(dao.readReview(sampleId), sampleId);
        RiskAssessment assessment = assess(data);
        LOG.info(() -> "Розраховано інтегральний показник для проби ID " + sampleId
                + " (" + assessment.index().toPlainString() + ").");
        return assessment;
    }

    public void changeState(long sampleId, SampleState desired) throws SQLException {
        ServiceValidation.requireId(sampleId);
        ServiceValidation.required(desired, "новий стан");
        SampleState previous;
        try {
            previous = ServiceValidation.found(
                    dao.changeState(sampleId, desired, data -> validateTransition(data, desired)), sampleId);
        } catch (IllegalStateException failure) {
            LOG.warning(() -> "Відхилено перехід проби ID " + sampleId + ": " + failure.getMessage());
            throw failure;
        }
        LOG.info(() -> "Змінено стан проби ID " + sampleId + " з " + previous + " на " + desired + ".");
    }

    public static boolean isAllowedTransition(SampleState current, SampleState desired) {
        if (current == null || desired == null) {
            return false;
        }
        return switch (current) {
            case REGISTERED -> desired == SampleState.VALIDATED;
            case VALIDATED -> desired == SampleState.ANALYZED;
            case ANALYZED -> desired == SampleState.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    private static void validateTransition(WaterSampleDao.Review data, SampleState desired) {
        SampleState current = data.sample().getState();
        if (!isAllowedTransition(current, desired)) {
            throw new IllegalStateException("Перехід " + current + " → " + desired + " заборонено.");
        }
        if (desired == SampleState.VALIDATED) {
            if (data.measurements().isEmpty()) {
                throw new IllegalStateException("Для валідації потрібно додати принаймні одне вимірювання.");
            }
            if (data.latestCheck().map(SampleCheck::getResult).orElse(null) != CheckResult.PASSED) {
                throw new IllegalStateException("Остання перевірка проби має бути успішною.");
            }
        }
        if (desired == SampleState.ANALYZED) {
            if (data.measurements().isEmpty()) {
                throw new IllegalStateException("Для аналізу потрібно принаймні одне вимірювання.");
            }
            assess(data);
        }
    }

    private static RiskAssessment assess(WaterSampleDao.Review data) {
        return RiskCalculator.assess(data.measurements(), data.point().getSensitivityScore());
    }

    private void validate(WaterSample value) throws SQLException {
        ServiceValidation.required(value, "проба");
        ServiceValidation.requireId(value.getPointId());
        ServiceValidation.requireText(value.getCode(), 24, "код проби");
        ServiceValidation.requireDateTime(value.getSampledAt(), "час відбору");
        if (points.getById(value.getPointId()).isEmpty()) {
            throw new IllegalArgumentException("Пункт спостереження для проби не знайдено.");
        }
    }
}
