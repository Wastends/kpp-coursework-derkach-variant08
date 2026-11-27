package ua.edu.nupp.kpp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class JsonEntityServlet<T> extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Logger LOG = Logger.getLogger(JsonEntityServlet.class.getName());
    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    record Result(int status, Object body) {
    }

    @FunctionalInterface
    private interface RequestAction {
        Result run() throws SQLException;
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            String action = request.getParameter("action");
            if (action != null) {
                return readAction(action, request);
            }
            Object data = request.getParameter("id") == null ? getAll() : getById(id(request));
            return new Result(HttpServletResponse.SC_OK, data);
        });
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> switch (required(request, "action")) {
            case "add" -> new Result(HttpServletResponse.SC_CREATED, Map.of("id", add(form(request, 0L))));
            case "update" -> {
                update(form(request, id(request)));
                yield success();
            }
            case "delete" -> {
                delete(id(request));
                yield success();
            }
            default -> writeAction(request.getParameter("action"), request);
        });
    }

    private void respond(HttpServletRequest request, HttpServletResponse response, RequestAction action)
            throws IOException {
        request.setCharacterEncoding(UTF_8);
        Result result;
        try {
            result = action.run();
        } catch (IllegalArgumentException | IllegalStateException error) {
            logRejected(request, HttpServletResponse.SC_BAD_REQUEST);
            result = error(HttpServletResponse.SC_BAD_REQUEST, error.getMessage());
        } catch (NoSuchElementException error) {
            logRejected(request, HttpServletResponse.SC_NOT_FOUND);
            result = error(HttpServletResponse.SC_NOT_FOUND, error.getMessage());
        } catch (SQLIntegrityConstraintViolationException error) {
            logRejected(request, HttpServletResponse.SC_BAD_REQUEST);
            result = error(HttpServletResponse.SC_BAD_REQUEST,
                    "Операцію відхилено. Перевірте унікальність полів і зв’язки між записами.");
        } catch (SQLException | RuntimeException error) {
            LOG.log(Level.SEVERE, "Не вдалося виконати " + request.getMethod() + " " + request.getServletPath(), error);
            result = error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера.");
        }

        response.setStatus(result.status());
        response.setCharacterEncoding(UTF_8);
        response.setContentType("application/json");
        JSON.writeValue(response.getWriter(), result.body());
    }

    private static void logRejected(HttpServletRequest request, int status) {
        LOG.warning(() -> "Відхилено " + request.getMethod() + " " + request.getServletPath() + " (HTTP " + status + ")");
    }

    private static Result error(int status, String message) {
        return new Result(status, Map.of("error", message == null ? "Помилка запиту." : message));
    }

    static Result success() {
        return new Result(HttpServletResponse.SC_OK, Map.of("ok", true));
    }

    Result readAction(String action, HttpServletRequest request) throws SQLException {
        throw new IllegalArgumentException("Невідома дія GET.");
    }

    Result writeAction(String action, HttpServletRequest request) throws SQLException {
        throw new IllegalArgumentException("Невідома дія POST.");
    }

    abstract List<T> getAll() throws SQLException;
    abstract T getById(long id) throws SQLException;
    abstract long add(T value) throws SQLException;
    abstract void update(T value) throws SQLException;
    abstract void delete(long id) throws SQLException;
    abstract T form(HttpServletRequest request, long id);

    static long id(HttpServletRequest request) {
        return longValue(request, "id");
    }

    static long longValue(HttpServletRequest request, String field) {
        try {
            return Long.parseLong(required(request, field));
        } catch (NumberFormatException error) {
            throw invalid(field);
        }
    }

    static int integer(HttpServletRequest request, String field) {
        try {
            return Integer.parseInt(required(request, field));
        } catch (NumberFormatException error) {
            throw invalid(field);
        }
    }

    static BigDecimal decimal(HttpServletRequest request, String field) {
        try {
            return new BigDecimal(required(request, field));
        } catch (NumberFormatException error) {
            throw invalid(field);
        }
    }

    static LocalDateTime dateTime(HttpServletRequest request, String field) {
        try {
            return LocalDateTime.parse(required(request, field));
        } catch (DateTimeParseException error) {
            throw invalid(field);
        }
    }

    static <E extends Enum<E>> E enumValue(HttpServletRequest request, String field, Class<E> type) {
        try {
            return Enum.valueOf(type, required(request, field));
        } catch (IllegalArgumentException error) {
            throw invalid(field);
        }
    }

    private static String required(HttpServletRequest request, String field) {
        String value = request.getParameter(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Обов’язковий параметр «" + field + "».");
        }
        return value.strip();
    }

    private static IllegalArgumentException invalid(String field) {
        return new IllegalArgumentException("Некоректне значення параметра «" + field + "».");
    }
}
