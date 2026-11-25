package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class WaterSampleDao {

    private static final String SELECT = "SELECT id, point_id, code, sampled_at, state FROM water_sample";

    public long add(WaterSample value) throws SQLException {
        String sql = "INSERT INTO water_sample (point_id, code, sampled_at, state) VALUES (?, ?, ?, ?)";
        return Jdbc.insert(sql, params -> {
            bind(params, value);
            params.setString(4, value.getState().name());
        });
    }

    public List<WaterSample> getAll() throws SQLException {
        return Jdbc.list(SELECT + " ORDER BY id", params -> {}, WaterSampleDao::map);
    }

    public Optional<WaterSample> getById(long id) throws SQLException {
        return Jdbc.one(SELECT + " WHERE id = ?", params -> params.setLong(1, id), WaterSampleDao::map);
    }

    public boolean update(WaterSample value) throws SQLException {
        String sql = "UPDATE water_sample SET point_id = ?, code = ?, sampled_at = ? WHERE id = ?";
        return Jdbc.write(sql, params -> {
            bind(params, value);
            params.setLong(4, value.getId());
        });
    }

    public boolean delete(long id) throws SQLException {
        return Jdbc.write("DELETE FROM water_sample WHERE id = ?", params -> params.setLong(1, id));
    }

    static Optional<WaterSample> findForReview(Connection connection, long id, boolean lock) throws SQLException {
        String sql = SELECT + " WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        return Jdbc.one(connection, sql, params -> params.setLong(1, id), WaterSampleDao::map);
    }

    record Review(WaterSample sample, ObservationPoint point, List<Measurement> measurements,
                  Optional<SampleCheck> latestCheck) {
    }

    Optional<Review> readReview(long id) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            Optional<Review> review = readReview(connection, id, false);
            connection.commit();
            return review;
        }
    }

    Optional<SampleState> changeState(long id, SampleState next, Consumer<Review> validate) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                Optional<Review> review = readReview(connection, id, true);
                if (review.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                Review data = review.get();
                validate.accept(data);
                boolean updated = Jdbc.write(connection, "UPDATE water_sample SET state = ? WHERE id = ?", params -> {
                    params.setString(1, next.name());
                    params.setLong(2, id);
                });
                if (!updated) {
                    throw new SQLException("Не вдалося оновити стан проби.");
                }
                connection.commit();
                return Optional.of(data.sample().getState());
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    private static Optional<Review> readReview(Connection connection, long id, boolean lock) throws SQLException {
        Optional<WaterSample> found = findForReview(connection, id, lock);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        WaterSample sample = found.get();
        ObservationPoint point = ObservationPointDao.findForReview(connection, sample.getPointId(), lock)
                .orElseThrow(() -> new SQLException("Пункт спостереження для проби відсутній."));
        return Optional.of(new Review(sample, point, MeasurementDao.findForReview(connection, id, lock),
                SampleCheckDao.findLatestForReview(connection, id, lock)));
    }

    private static void bind(PreparedStatement params, WaterSample value) throws SQLException {
        params.setLong(1, value.getPointId());
        params.setString(2, value.getCode());
        params.setObject(3, value.getSampledAt());
    }

    private static WaterSample map(ResultSet row) throws SQLException {
        return new WaterSample(
                row.getLong("id"),
                row.getLong("point_id"),
                row.getString("code"),
                row.getObject("sampled_at", LocalDateTime.class),
                SampleState.valueOf(row.getString("state"))
        );
    }
}
