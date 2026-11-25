package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public final class MeasurementServlet extends JsonEntityServlet<Measurement> {

    private final MeasurementService service = new MeasurementService();

    @Override
    List<Measurement> getAll() throws SQLException {
        return service.getAll();
    }

    @Override
    Measurement getById(long id) throws SQLException {
        return service.getById(id);
    }

    @Override
    long add(Measurement value) throws SQLException {
        return service.add(value);
    }

    @Override
    void update(Measurement value) throws SQLException {
        service.update(value);
    }

    @Override
    void delete(long id) throws SQLException {
        service.delete(id);
    }

    @Override
    Measurement form(HttpServletRequest request, long id) {
        return new Measurement(id, longValue(request, "sampleId"), request.getParameter("parameterName"),
                request.getParameter("unit"), decimal(request, "measuredValue"), decimal(request, "limitValue"));
    }
}
