package com.service;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Clients {
    private final String ROOT_PATH = "server/Storage";
    private Path userPath;

    public Path getUserPath() {
        return userPath;
    }

    public void authorization(ChannelHandlerContext ctx, String login, String password) {
        String path = AuthService.checkAuthorization(login,password);
        if (path != null) {
            userPath = Path.of(ROOT_PATH, path);
            ctx.writeAndFlush("/auth\nok");
        } else {
            ctx.writeAndFlush("/auth\nnoSuch");
        }
    }

    public void registration(ChannelHandlerContext ctx, String login, String password) {
        if (AuthService.checkLogin(login) != null) {
            ctx.writeAndFlush("/login\nbusy");
        } else {
            String currentPath = AuthService.tryRegister(login, password);
            System.out.println(currentPath);
            Path newPath = Path.of(ROOT_PATH, currentPath);
            if (!Files.exists(newPath)) {
                try {
                    Files.createDirectory(newPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            userPath = newPath;
            ctx.writeAndFlush("/auth\nok");
        }
    }
}
