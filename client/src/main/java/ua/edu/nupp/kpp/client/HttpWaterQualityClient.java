package ua.edu.nupp.kpp.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

public final class HttpWaterQualityClient {

    public static final String DEFAULT_URL = "http://localhost:8080/water-quality-server";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private final URI base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    final Resource<ObservationPoint> points = new Resource<>("/points", ObservationPoint.class,
            ObservationPoint::getId, value -> Map.of("code", value.getCode(), "name", value.getName(),
                    "location", value.getLocation(), "waterUse", value.getWaterUse(),
                    "sensitivityScore", String.valueOf(value.getSensitivityScore())));
    final Resource<WaterSample> samples = new Resource<>("/samples", WaterSample.class, WaterSample::getId,
            value -> Map.of("pointId", String.valueOf(value.getPointId()), "code", value.getCode(),
                    "sampledAt", value.getSampledAt().toString()));
    final Resource<Measurement> measurements = new Resource<>("/measurements", Measurement.class, Measurement::getId,
            value -> Map.of("sampleId", String.valueOf(value.getSampleId()), "parameterName", value.getParameterName(),
                    "unit", value.getUnit(), "measuredValue", value.getMeasuredValue().toString(),
                    "limitValue", value.getLimitValue().toString()));
    final Resource<SampleCheck> checks = new Resource<>("/checks", SampleCheck.class, SampleCheck::getId,
            value -> Map.of("sampleId", String.valueOf(value.getSampleId()), "checkedAt", value.getCheckedAt().toString(),
                    "result", value.getResult().name(), "note", value.getNote()));

    public HttpWaterQualityClient(String baseUrl) {
        base = URI.create(baseUrl.strip().replaceFirst("/+$", ""));
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getRawQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Укажіть повну HTTP-адресу сервера без параметрів.");
        }
    }

    public RiskAssessment risk(long id) throws IOException, InterruptedException {
        return read(get("/samples?action=risk&id=" + id), RiskAssessment.class);
    }

    public void transition(long id, SampleState next) throws IOException, InterruptedException {
        requireSuccess(post("/samples", Map.of("action", "transition", "id", String.valueOf(id), "state", next.name())));
    }

    public final class Resource<T> {

        private final String path;
        private final Class<T> type;
        private final ToLongFunction<T> id;
        private final Function<T, Map<String, String>> fields;

        private Resource(String path, Class<T> type, ToLongFunction<T> id, Function<T, Map<String, String>> fields) {
            this.path = path;
            this.type = type;
            this.id = id;
            this.fields = fields;
        }

        public List<T> list() throws IOException, InterruptedException {
            String body = get(path);
            try {
                List<T> values = json.readerForListOf(type).readValue(body);
                if (values == null) {
                    throw new IOException("Сервер повернув порожню відповідь замість списку.");
                }
                return values;
            } catch (JsonProcessingException error) {
                throw new IOException("Сервер повернув список у невідомому форматі JSON.", error);
            }
        }

        public T getById(long id) throws IOException, InterruptedException {
            return read(get(path + "?id=" + id), type);
        }

        public long add(T value) throws IOException, InterruptedException {
            long generatedId = read(post(path, form("add", value)), Created.class).id();
            if (generatedId <= 0) {
                throw new IOException("Сервер не повернув ідентифікатор нового запису.");
            }
            return generatedId;
        }

        public void update(T value) throws IOException, InterruptedException {
            Map<String, String> params = form("update", value);
            params.put("id", String.valueOf(id.applyAsLong(value)));
            requireSuccess(post(path, params));
        }

        public void delete(long id) throws IOException, InterruptedException {
            requireSuccess(post(path, Map.of("action", "delete", "id", String.valueOf(id))));
        }

        private Map<String, String> form(String action, T value) {
            Map<String, String> params = new HashMap<>(fields.apply(value));
            params.put("action", action);
            return params;
        }
    }

    private record Created(long id) {
    }

    private record Acknowledgement(boolean ok) {
    }

    private void requireSuccess(String body) throws IOException {
        if (!read(body, Acknowledgement.class).ok()) {
            throw new IOException("Сервер не підтвердив виконання операції.");
        }
    }

    private String get(String path) throws IOException, InterruptedException {
        return send(request(path).GET().build());
    }

    private String post(String path, Map<String, String> params) throws IOException, InterruptedException {
        String form = params.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return send(request(path).header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8)).build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(base.toString() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException error) {
            throw new IOException("Сервер недоступний. Перевірте, чи запущено Tomcat.", error);
        }
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException(serverError(status, response.body()));
        }
        return response.body();
    }

    private String serverError(int status, String body) {
        try {
            JsonNode jsonBody = json.readTree(body);
            if (jsonBody != null && jsonBody.path("error").isTextual()) {
                return jsonBody.path("error").textValue() + " (HTTP " + status + ")";
            }
        } catch (JsonProcessingException ignored) {
            // Tomcat may reply with HTML if the application has not been deployed.
        }
        return "Сервер повернув HTTP " + status + ". Перевірте адресу та стан вебзастосунку.";
    }

    private <T> T read(String body, Class<T> type) throws IOException {
        try {
            T value = json.readValue(body, type);
            if (value == null) {
                throw new IOException("Сервер повернув порожню відповідь JSON.");
            }
            return value;
        } catch (JsonProcessingException error) {
            throw new IOException("Сервер повернув відповідь у невідомому форматі JSON.", error);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
