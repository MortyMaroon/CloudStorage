package com.handlers;

import com.service.AuthService;
import com.utils.FileService;
import com.utils.Signal;
import com.utils.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.service.Clients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChatMessageHandler extends ChannelInboundHandlerAdapter {
    private State currentState = State.WAIT;
    private int commandLength = 0;
    private int filenameLength = 0;
    private long fileSize = 0L;
    private long receivedFileSize = 0L;
    private Clients user = new Clients();
    private StringBuilder builder;
    private Path userPath;
    private FileService fileService = new FileService();


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        while (byteBuf.readableBytes() > 0) {
            if (currentState == State.WAIT) {
                byte readByte = byteBuf.readByte();
                if (readByte == Signal.COMMAND) {
                    currentState = State.COMMAND;
                    System.out.println("прилетел сигнальный байт");
                } else if (readByte == Signal.FILE) {
//                    currentState =
                    System.out.println("начало загрузки");
                } else {
                    currentState = State.WAIT;
                    System.out.println("Неизвестная команда");
                }
            }

            if (currentState == State.COMMAND) {
                if (byteBuf.readableBytes() >= 4) {
                    System.out.println("читаем длину команды");
                    commandLength = byteBuf.readInt();
                    System.out.println("длинна команды: " + commandLength);
                    currentState = State.COMMAND_READ;
                }
            }

            if (currentState == State.COMMAND_READ) {
                builder = new StringBuilder();
                System.out.println("читаем команду по символам: ");
                while (byteBuf.readableBytes() > 0 && commandLength != 0) {
                    char symbol = (char) byteBuf.readByte();
                    System.out.print(symbol);
                    builder.append(symbol);
                    commandLength--;
                }
                currentState = State.COMMAND_DOING;
            }

            if (currentState == State.COMMAND_DOING) {
                String[] cmd = builder.toString().split("\n");
                switch (cmd[0]) {
                    case "/auth":
                        System.out.println();
                        System.out.println("Client try authorization");
                        String path = AuthService.checkAuthorization(cmd[1],cmd[2]);
                        if (path != null) {
                            userPath = Path.of("server/Storage", path);
                            fileService.sendCommand(ctx.channel(), "/auth\nok");
                            System.out.println("клиент авторизировался");
                        } else {
                            ctx.channel().writeAndFlush("/auth\nnoSuch".getBytes());
                        }
                        currentState = State.WAIT;
                        break;
                    case "/reg":
                        System.out.println("Client try registration");
                        if (AuthService.checkLogin(cmd[1]) != null) {
                            ctx.writeAndFlush("/login\nbusy");
                        } else {
                            String currentPath = AuthService.tryRegister(cmd[1], cmd[2]);
                            System.out.println(currentPath);
                            Path newPath = Path.of("server/Storage", currentPath);
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
                        currentState = State.WAIT;
                        break;
                    case "exit":
                        ctx.writeAndFlush("exitOk");
                        ctx.close();
                        break;
                }
            }
        }
    }
}
