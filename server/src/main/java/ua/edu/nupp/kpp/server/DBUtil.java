package ua.edu.nupp.kpp.server;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DBUtil {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kpp_water_quality_v08";
    private static final MysqlDataSource DATA_SOURCE = configureDataSource();

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    private static MysqlDataSource configureDataSource() {
        MysqlDataSource source = new MysqlDataSource();
        source.setURL(System.getenv().getOrDefault("KPP_DB_URL", DEFAULT_URL));
        source.setUser(System.getenv().getOrDefault("KPP_DB_USER", "root"));
        source.setPassword(System.getenv().getOrDefault("KPP_DB_PASSWORD", ""));
        return source;
    }
}
