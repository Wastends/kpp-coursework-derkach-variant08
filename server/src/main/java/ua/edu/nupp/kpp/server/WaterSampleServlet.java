package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

public final class WaterSampleServlet extends JsonEntityServlet<WaterSample> {

    private final WaterSampleService service = new WaterSampleService();

    @Override
    List<WaterSample> getAll() throws SQLException {
        return service.getAll();
    }

    @Override
    WaterSample getById(long id) throws SQLException {
        return service.getById(id);
    }

    @Override
    long add(WaterSample value) throws SQLException {
        return service.add(value);
    }

    @Override
    void update(WaterSample value) throws SQLException {
        service.update(value);
    }

    @Override
    void delete(long id) throws SQLException {
        service.delete(id);
    }

    @Override
    WaterSample form(HttpServletRequest request, long id) {
        return new WaterSample(id, longValue(request, "pointId"), request.getParameter("code"),
                dateTime(request, "sampledAt"), null);
    }

    @Override
    Result readAction(String action, HttpServletRequest request) throws SQLException {
        if ("risk".equals(action)) {
            return new Result(HttpServletResponse.SC_OK, service.calculateRisk(id(request)));
        }
        return super.readAction(action, request);
    }

    @Override
    Result writeAction(String action, HttpServletRequest request) throws SQLException {
        if ("transition".equals(action)) {
            service.changeState(id(request), enumValue(request, "state", SampleState.class));
            return success();
        }
        return super.writeAction(action, request);
    }
}
