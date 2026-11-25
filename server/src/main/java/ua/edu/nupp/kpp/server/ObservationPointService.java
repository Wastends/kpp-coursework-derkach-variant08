package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public final class ObservationPointService {

    private static final Logger LOG = Logger.getLogger(ObservationPointService.class.getName());

    private final ObservationPointDao dao = new ObservationPointDao();

    public long add(ObservationPoint value) throws SQLException {
        validate(value);
        long id = dao.add(value);
        LOG.info(() -> "Додано пункт спостереження ID " + id + ".");
        return id;
    }

    public List<ObservationPoint> getAll() throws SQLException {
        return dao.getAll();
    }

    public ObservationPoint getById(long id) throws SQLException {
        ServiceValidation.requireId(id);
        return ServiceValidation.found(dao.getById(id), id);
    }

    public void update(ObservationPoint value) throws SQLException {
        validate(value);
        ServiceValidation.requireId(value.getId());
        ServiceValidation.requireChanged(dao.update(value), value.getId());
        LOG.info(() -> "Оновлено пункт спостереження ID " + value.getId() + ".");
    }

    public void delete(long id) throws SQLException {
        ServiceValidation.requireId(id);
        ServiceValidation.requireChanged(dao.delete(id), id);
        LOG.info(() -> "Видалено пункт спостереження ID " + id + ".");
    }

    private static void validate(ObservationPoint value) {
        ServiceValidation.required(value, "пункт спостереження");
        ServiceValidation.requireText(value.getCode(), 24, "код пункту");
        ServiceValidation.requireText(value.getName(), 120, "назва");
        ServiceValidation.requireText(value.getLocation(), 180, "розташування");
        ServiceValidation.requireText(value.getWaterUse(), 80, "вид водокористування");
        int score = value.getSensitivityScore();
        if (score != 25 && score != 50 && score != 75 && score != 100) {
            throw new IllegalArgumentException("Чутливість водокористування має становити 25, 50, 75 або 100.");
        }
    }
}
