package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBUtil {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kpp_water_quality_v08";

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("KPP_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("KPP_DB_USER", "root");
        String password = System.getenv().getOrDefault("KPP_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
