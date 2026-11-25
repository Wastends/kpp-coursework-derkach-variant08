package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public final class MeasurementService {

    private static final Logger LOG = Logger.getLogger(MeasurementService.class.getName());

    private final MeasurementDao dao = new MeasurementDao();
    private final WaterSampleDao samples = new WaterSampleDao();

    public long add(Measurement value) throws SQLException {
        validate(value);
        long id = dao.add(value);
        LOG.info(() -> "Додано вимірювання ID " + id + ".");
        return id;
    }

    public List<Measurement> getAll() throws SQLException {
        return dao.getAll();
    }

    public Measurement getById(long id) throws SQLException {
        ServiceValidation.requireId(id);
        return ServiceValidation.found(dao.getById(id), id);
    }

    public void update(Measurement value) throws SQLException {
        validate(value);
        ServiceValidation.requireId(value.getId());
        ServiceValidation.requireChanged(dao.update(value), value.getId());
        LOG.info(() -> "Оновлено вимірювання ID " + value.getId() + ".");
    }

    public void delete(long id) throws SQLException {
        ServiceValidation.requireId(id);
        ServiceValidation.requireChanged(dao.delete(id), id);
        LOG.info(() -> "Видалено вимірювання ID " + id + ".");
    }

    private void validate(Measurement value) throws SQLException {
        ServiceValidation.required(value, "вимірювання");
        ServiceValidation.requireId(value.getSampleId());
        ServiceValidation.requireText(value.getParameterName(), 80, "назва параметра");
        ServiceValidation.requireText(value.getUnit(), 20, "одиниця вимірювання");
        ServiceValidation.requireDecimal(value.getMeasuredValue(), "виміряне значення");
        ServiceValidation.requireDecimal(value.getLimitValue(), "контрольна межа");
        if (value.getMeasuredValue().signum() < 0 || value.getLimitValue().signum() <= 0) {
            throw new IllegalArgumentException("Виміряне значення має бути невід’ємним, а контрольна межа додатною.");
        }
        if (samples.getById(value.getSampleId()).isEmpty()) {
            throw new IllegalArgumentException("Пробу для вимірювання не знайдено.");
        }
    }
}
