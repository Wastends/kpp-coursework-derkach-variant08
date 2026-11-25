package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public final class ObservationPointServlet extends JsonEntityServlet<ObservationPoint> {

    private final ObservationPointService service = new ObservationPointService();

    @Override
    List<ObservationPoint> getAll() throws SQLException {
        return service.getAll();
    }

    @Override
    ObservationPoint getById(long id) throws SQLException {
        return service.getById(id);
    }

    @Override
    long add(ObservationPoint value) throws SQLException {
        return service.add(value);
    }

    @Override
    void update(ObservationPoint value) throws SQLException {
        service.update(value);
    }

    @Override
    void delete(long id) throws SQLException {
        service.delete(id);
    }

    @Override
    ObservationPoint form(HttpServletRequest request, long id) {
        return new ObservationPoint(id, request.getParameter("code"), request.getParameter("name"),
                request.getParameter("location"), request.getParameter("waterUse"), integer(request, "sensitivityScore"));
    }
}
