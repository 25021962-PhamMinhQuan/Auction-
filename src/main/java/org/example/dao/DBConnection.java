package org.example.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.*;
import java.util.Properties;

public class DBConnection {
    private static String url;
    private static String username;
    private static String password;

    static{
        try{
            Properties props = new Properties();
            InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config.properties");
            if(input == null){
                throw new RuntimeException("config not found");
            }
            props.load(input);
            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

        } catch (IOException e) {
            throw new RuntimeException("can't read config", e);
        }
    }
    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url,username,password);
    }
}
