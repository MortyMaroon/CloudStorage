package com.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement statement;
    private static final String DATABASE_NAME = "StorageServer.db";
    private static final String URL = "jdbc:sqlite:server/src/main/resources/" + DATABASE_NAME;
    private static final String ROOT_PATH = "server/Storage";

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getRootPath() {
        return ROOT_PATH;
    }

    public static Path authorization(String login, String password) {
        String path = checkAuthorization(login,password);
        if (path != null) {
            return Path.of(ROOT_PATH, path);
        } else {
            return null;
        }
    }

    public static Path registration(String login, String password) {
        if (checkLogin(login) != null) {
            return null;
        } else {
            Path newPath = Path.of(ROOT_PATH, tryRegister(login, password));
            if (!Files.exists(newPath)) {
                try {
                    if (!Files.exists(newPath)) {
                        Files.createDirectory(newPath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return newPath;
        }
    }

    private static String checkAuthorization(String login, String password) {
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

    private static String checkLogin(String login) {
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

    private static String tryRegister(String login, String password) {
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
