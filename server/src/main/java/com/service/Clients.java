package com.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Clients {
    private final String ROOT_PATH = "server/Storage";

    public String getROOT_PATH() {
        return ROOT_PATH;
    }

    public Path authorization(String login, String password) {
        String path = AuthService.checkAuthorization(login,password);
        if (path != null) {
            return Path.of(ROOT_PATH, path);
        } else {
            return null;
        }
    }

    public Path registration(String login, String password) {
        if (AuthService.checkLogin(login) != null) {
            return null;
        } else {
            Path newPath = Path.of(ROOT_PATH, AuthService.tryRegister(login, password));
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
}
