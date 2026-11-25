package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public final class SampleCheckService {

    private static final Logger LOG = Logger.getLogger(SampleCheckService.class.getName());

    private final SampleCheckDao dao = new SampleCheckDao();
    private final WaterSampleDao samples = new WaterSampleDao();

    public long add(SampleCheck value) throws SQLException {
        validate(value);
        long id = dao.add(value);
        LOG.info(() -> "Додано перевірку ID " + id + ".");
        return id;
    }

    public List<SampleCheck> getAll() throws SQLException {
        return dao.getAll();
    }

    public SampleCheck getById(long id) throws SQLException {
        ServiceValidation.requireId(id);
        return ServiceValidation.found(dao.getById(id), id);
    }

    public void update(SampleCheck value) throws SQLException {
        validate(value);
        ServiceValidation.requireId(value.getId());
        ServiceValidation.requireChanged(dao.update(value), value.getId());
        LOG.info(() -> "Оновлено перевірку ID " + value.getId() + ".");
    }

    public void delete(long id) throws SQLException {
        ServiceValidation.requireId(id);
        ServiceValidation.requireChanged(dao.delete(id), id);
        LOG.info(() -> "Видалено перевірку ID " + id + ".");
    }

    private void validate(SampleCheck value) throws SQLException {
        ServiceValidation.required(value, "перевірка");
        ServiceValidation.requireId(value.getSampleId());
        ServiceValidation.requireDateTime(value.getCheckedAt(), "час перевірки");
        ServiceValidation.required(value.getResult(), "результат перевірки");
        ServiceValidation.requireText(value.getNote(), 300, "примітка");
        if (samples.getById(value.getSampleId()).isEmpty()) {
            throw new IllegalArgumentException("Пробу для перевірки не знайдено.");
        }
    }
}
