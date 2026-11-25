package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public final class SampleCheckServlet extends JsonEntityServlet<SampleCheck> {

    private final SampleCheckService service = new SampleCheckService();

    @Override
    List<SampleCheck> getAll() throws SQLException {
        return service.getAll();
    }

    @Override
    SampleCheck getById(long id) throws SQLException {
        return service.getById(id);
    }

    @Override
    long add(SampleCheck value) throws SQLException {
        return service.add(value);
    }

    @Override
    void update(SampleCheck value) throws SQLException {
        service.update(value);
    }

    @Override
    void delete(long id) throws SQLException {
        service.delete(id);
    }

    @Override
    SampleCheck form(HttpServletRequest request, long id) {
        return new SampleCheck(id, longValue(request, "sampleId"), dateTime(request, "checkedAt"),
                enumValue(request, "result", CheckResult.class), request.getParameter("note"));
    }
}
