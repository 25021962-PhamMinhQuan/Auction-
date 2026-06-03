package org.example.repository.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

public class DBConnection {
    private static HikariDataSource ds;

    static {
        try {
            Properties props = new Properties();
            try(InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) throw new RuntimeException("config.properties not found");
                props.load(input);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName(props.getProperty("db.driver"));

            config.setMaximumPoolSize(3); // Giữ sẵn 10 kết nối luôn mở
            config.setConnectionTimeout(30000); // Đợi tối đa 30s
            config.setMinimumIdle(0);
            config.setIdleTimeout(10000);

            ds = new HikariDataSource(config);
            System.out.println("Connection Pool initialization successful!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Connection Pool Configuration Error", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}