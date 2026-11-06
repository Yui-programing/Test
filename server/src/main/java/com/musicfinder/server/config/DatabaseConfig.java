package com.musicfinder.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final HikariDataSource dataSource;
    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("Khong the load duoc application.properties, dung cau hinh fallback");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url")); // lay link database
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));

        //Toi uu hoa cho pool tcp server
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.size")));
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);

        dataSource = new HikariDataSource(config);
    }
     // Lấy 1 connection từ pool
    public static Connection getConnection() throws SQLException{
        return dataSource.getConnection();
    }
    // Đóng pool khi tắt server
    public static void shutdown(){
        if(dataSource !=null && !dataSource.isClosed()){
            dataSource.close();
        }
    }
}
