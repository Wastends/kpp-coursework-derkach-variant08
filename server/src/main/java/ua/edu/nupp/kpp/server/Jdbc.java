package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Jdbc {

    @FunctionalInterface
    interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet row) throws SQLException;
    }

    private Jdbc() {
    }

    static <T> List<T> list(String sql, Binder binder, RowMapper<T> mapper) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            return list(connection, sql, binder, mapper);
        }
    }

    static <T> List<T> list(Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(mapper.map(rows));
                }
                return results;
            }
        }
    }

    static <T> Optional<T> one(String sql, Binder binder, RowMapper<T> mapper) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            return one(connection, sql, binder, mapper);
        }
    }

    static <T> Optional<T> one(Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapper.map(rows)) : Optional.empty();
            }
        }
    }

    static long insert(String sql, Binder binder) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("База даних не повернула ідентифікатор нового запису.");
                }
                return keys.getLong(1);
            }
        }
    }

    static boolean write(String sql, Binder binder) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            return write(connection, sql, binder);
        }
    }

    static boolean write(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate() == 1;
        }
    }
}
