package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class SampleCheckDao {

    private static final String SELECT = "SELECT id, sample_id, checked_at, result, note FROM sample_check";

    public long add(SampleCheck value) throws SQLException {
        String sql = "INSERT INTO sample_check (sample_id, checked_at, result, note) VALUES (?, ?, ?, ?)";
        return Jdbc.insert(sql, params -> bind(params, value));
    }

    public List<SampleCheck> getAll() throws SQLException {
        return Jdbc.list(SELECT + " ORDER BY id", params -> {}, SampleCheckDao::map);
    }

    public Optional<SampleCheck> getById(long id) throws SQLException {
        return Jdbc.one(SELECT + " WHERE id = ?", params -> params.setLong(1, id), SampleCheckDao::map);
    }

    public boolean update(SampleCheck value) throws SQLException {
        String sql = "UPDATE sample_check SET sample_id = ?, checked_at = ?, result = ?, note = ? WHERE id = ?";
        return Jdbc.write(sql, params -> {
            bind(params, value);
            params.setLong(5, value.getId());
        });
    }

    public boolean delete(long id) throws SQLException {
        return Jdbc.write("DELETE FROM sample_check WHERE id = ?", params -> params.setLong(1, id));
    }

    static Optional<SampleCheck> findLatestForReview(Connection connection, long sampleId, boolean lock)
            throws SQLException {
        String sql = SELECT + " WHERE sample_id = ? ORDER BY checked_at DESC, id DESC LIMIT 1"
                + (lock ? " FOR UPDATE" : "");
        return Jdbc.one(connection, sql, params -> params.setLong(1, sampleId), SampleCheckDao::map);
    }

    private static void bind(PreparedStatement params, SampleCheck value) throws SQLException {
        params.setLong(1, value.getSampleId());
        params.setObject(2, value.getCheckedAt());
        params.setString(3, value.getResult().name());
        params.setString(4, value.getNote());
    }

    private static SampleCheck map(ResultSet row) throws SQLException {
        return new SampleCheck(
                row.getLong("id"),
                row.getLong("sample_id"),
                row.getObject("checked_at", LocalDateTime.class),
                CheckResult.valueOf(row.getString("result")),
                row.getString("note")
        );
    }
}
