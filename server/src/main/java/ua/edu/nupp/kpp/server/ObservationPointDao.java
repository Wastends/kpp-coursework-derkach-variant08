package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class ObservationPointDao {

    private static final String SELECT = "SELECT id, code, name, location, water_use, sensitivity_score FROM observation_point";

    public long add(ObservationPoint value) throws SQLException {
        String sql = "INSERT INTO observation_point (code, name, location, water_use, sensitivity_score) VALUES (?, ?, ?, ?, ?)";
        return Jdbc.insert(sql, params -> bind(params, value));
    }

    public List<ObservationPoint> getAll() throws SQLException {
        return Jdbc.list(SELECT + " ORDER BY id", params -> {}, ObservationPointDao::map);
    }

    public Optional<ObservationPoint> getById(long id) throws SQLException {
        return Jdbc.one(SELECT + " WHERE id = ?", params -> params.setLong(1, id), ObservationPointDao::map);
    }

    public boolean update(ObservationPoint value) throws SQLException {
        String sql = "UPDATE observation_point SET code = ?, name = ?, location = ?, water_use = ?, sensitivity_score = ? WHERE id = ?";
        return Jdbc.write(sql, params -> {
            bind(params, value);
            params.setLong(6, value.getId());
        });
    }

    public boolean delete(long id) throws SQLException {
        return Jdbc.write("DELETE FROM observation_point WHERE id = ?", params -> params.setLong(1, id));
    }

    static Optional<ObservationPoint> findForReview(Connection connection, long id, boolean lock) throws SQLException {
        String sql = SELECT + " WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        return Jdbc.one(connection, sql, params -> params.setLong(1, id), ObservationPointDao::map);
    }

    private static void bind(PreparedStatement params, ObservationPoint value) throws SQLException {
        params.setString(1, value.getCode());
        params.setString(2, value.getName());
        params.setString(3, value.getLocation());
        params.setString(4, value.getWaterUse());
        params.setInt(5, value.getSensitivityScore());
    }

    private static ObservationPoint map(ResultSet row) throws SQLException {
        return new ObservationPoint(
                row.getLong("id"),
                row.getString("code"),
                row.getString("name"),
                row.getString("location"),
                row.getString("water_use"),
                row.getInt("sensitivity_score")
        );
    }
}
