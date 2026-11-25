package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class MeasurementDao {

    private static final String SELECT = "SELECT id, sample_id, parameter_name, unit, measured_value, limit_value FROM measurement";

    public long add(Measurement value) throws SQLException {
        String sql = "INSERT INTO measurement (sample_id, parameter_name, unit, measured_value, limit_value) VALUES (?, ?, ?, ?, ?)";
        return Jdbc.insert(sql, params -> bind(params, value));
    }

    public List<Measurement> getAll() throws SQLException {
        return Jdbc.list(SELECT + " ORDER BY id", params -> {}, MeasurementDao::map);
    }

    public Optional<Measurement> getById(long id) throws SQLException {
        return Jdbc.one(SELECT + " WHERE id = ?", params -> params.setLong(1, id), MeasurementDao::map);
    }

    public boolean update(Measurement value) throws SQLException {
        String sql = "UPDATE measurement SET sample_id = ?, parameter_name = ?, unit = ?, measured_value = ?, limit_value = ? WHERE id = ?";
        return Jdbc.write(sql, params -> {
            bind(params, value);
            params.setLong(6, value.getId());
        });
    }

    public boolean delete(long id) throws SQLException {
        return Jdbc.write("DELETE FROM measurement WHERE id = ?", params -> params.setLong(1, id));
    }

    static List<Measurement> findForReview(Connection connection, long sampleId, boolean lock) throws SQLException {
        String sql = SELECT + " WHERE sample_id = ? ORDER BY id" + (lock ? " FOR UPDATE" : "");
        return Jdbc.list(connection, sql, params -> params.setLong(1, sampleId), MeasurementDao::map);
    }

    private static void bind(PreparedStatement params, Measurement value) throws SQLException {
        params.setLong(1, value.getSampleId());
        params.setString(2, value.getParameterName());
        params.setString(3, value.getUnit());
        params.setBigDecimal(4, value.getMeasuredValue());
        params.setBigDecimal(5, value.getLimitValue());
    }

    private static Measurement map(ResultSet row) throws SQLException {
        return new Measurement(
                row.getLong("id"),
                row.getLong("sample_id"),
                row.getString("parameter_name"),
                row.getString("unit"),
                row.getBigDecimal("measured_value"),
                row.getBigDecimal("limit_value")
        );
    }
}
