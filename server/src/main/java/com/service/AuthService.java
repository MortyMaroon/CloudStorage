package com.service;

import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement statement;
    private static final String DATABASE_NAME = "StorageServer.db";
    private static final String URL = "jdbc:sqlite:server/src/com.main/resources/" + DATABASE_NAME;

    public static Statement getStatement() {
        return statement;
    }

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String checkAuthorization(String login, String password) {
        String sql = String.format("SELECT path FROM users WHERE login = '%s' AND password = '%s'", login, password);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return rs.getString("path");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String checkLogin(String login) {
        String sql = String.format("SELECT login FROM users WHERE login = '%s'", login);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return rs.getString("login");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String tryRegister(String login, String password) {
        String sql = String.format("INSERT INTO users(login, password, path) VALUES('%s','%s','%s')", login, password, login);
        try {
            int row = statement.executeUpdate(sql);
            if (row >= 1) {
                return login;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
