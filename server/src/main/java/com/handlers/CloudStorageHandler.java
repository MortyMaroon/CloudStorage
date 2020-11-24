package com.handlers;

import com.service.AuthService;
import com.utils.FileInfo;
import com.service.MessageService;
import com.utils.Signal;
import com.utils.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CloudStorageHandler extends ChannelInboundHandlerAdapter {
    private State currentState = State.WAIT;
    private int commandLength = 0;
    private int filenameLength = 0;
    private long fileSize = 0L;
    private long beRead = 0L;
    private BufferedOutputStream outFile;
    private StringBuilder builder;
    private Path userPath;
    private final MessageService fileService = new MessageService();

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
                byte signalByte = byteBuf.readByte();
                if (signalByte == Signal.COMMAND) {
                    currentState = State.COMMAND;
                } else if (signalByte == Signal.FILE) {
                    currentState = State.FILE;
                    System.out.println("Start downloading...");
                } else {
                    currentState = State.WAIT;
                    throw new RuntimeException("Unknown byte command: " + signalByte);
                }
            }

            if (currentState == State.COMMAND) {
                if (byteBuf.readableBytes() >= 4) {
                    commandLength = byteBuf.readInt();
                    currentState = State.COMMAND_READ;
                }
            }

            if (currentState == State.COMMAND_READ) {
                builder = new StringBuilder();
                while (byteBuf.readableBytes() > 0 && commandLength != 0) {
                    char symbol = (char) byteBuf.readByte();
                    builder.append(symbol);
                    commandLength--;
                }
                System.out.println();
                currentState = State.COMMAND_DOING;
            }

            if (currentState == State.COMMAND_DOING) {
                String[] cmd = builder.toString().split("\n");
                switch (cmd[0]) {

                    case "/auth":
                        Path path = AuthService.authorization(cmd[1],cmd[2]);
                        if (path != null) {
                            userPath = path;
                            fileService.sendCommand(ctx.channel(), "/auth\nok");
                        } else {
                            fileService.sendCommand(ctx.channel(), "/auth\nnoSuch");
                        }
                        currentState = State.WAIT;
                        break;

                    case "/reg":
                        Path newPath = AuthService.registration(cmd[1],cmd[2]);
                        if (newPath != null) {
                            userPath = newPath;
                            fileService.sendCommand(ctx.channel(), "/auth\nok");
                        } else {
                            fileService.sendCommand(ctx.channel(), "/login\nbusy");
                        }
                        currentState = State.WAIT;
                        break;

                    case "exit":
                        fileService.sendCommand(ctx.channel(), "exit\nOk");
                        ctx.close();
                        break;

                    case "/updateFileList":
                        currentState = State.UPDATE_FILE_LIST;
                        break;

                    case "/enterToDirectory":
                        userPath = userPath.resolve(cmd[1]);
                        currentState = State.UPDATE_FILE_LIST;
                        break;

                    case "/upDirectory":
                        if (userPath.getParent().toString().equals(AuthService.getRootPath())){
                            currentState = State.WAIT;
                        } else {
                            userPath = userPath.getParent();
                            currentState = State.UPDATE_FILE_LIST;
                        }
                        break;

                    case "/delete":
                        try {
                            Files.delete(userPath.resolve(cmd[1]));
                            currentState = State.UPDATE_FILE_LIST;
                            break;
                        } catch (IOException exception) {
                            fileService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getMessage()));
                            currentState = State.WAIT;
                            break;
                        }

                    case "/download":
                        try {
                            fileService.uploadFile(ctx.channel(), userPath.resolve(cmd[1]), cmd[2]);
                            currentState = State.WAIT;
                            break;
                        } catch (IOException exception) {
                            fileService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getMessage()));
                            currentState = State.WAIT;
                            break;
                        }

                    case "/disconnect":
                        userPath = null;
                        fileService.sendCommand(ctx.channel(), "/disconnect\nOk");
                        currentState = State.WAIT;
                        break;

                    case "/mkdir":
                        try {
                            File directory = new File(userPath + File.separator + cmd[1]);
                            if (directory.exists()) {
                                throw new Exception("Directory is already exist");
                            } else {
                                directory.mkdir();
                            }
                        } catch (Exception exception) {
                            fileService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getCause().getMessage()));
                            currentState = State.WAIT;
                            break;
                        }
                }
            }

            if (currentState == State.FILE) {
                beRead = 0L;
                fileSize = 0L;

                System.out.println("Читаем длинну имени файла");
                filenameLength = byteBuf.readInt();
                System.out.println("Длинна имени файла равна: " + filenameLength);

                System.out.println("Читаем имя файла");
                byte[] filenameInBytes = new byte[filenameLength];
                byteBuf.readBytes(filenameInBytes);
                String filename = new String(filenameInBytes, StandardCharsets.UTF_8);
                System.out.println("Имя файла: " + filename);

                System.out.println(userPath.toString());
                File downloadFile = new File(userPath.toString() + File.separator + filename);
                outFile = new BufferedOutputStream(new FileOutputStream(downloadFile));

                System.out.println("Читаем длинну файла");
                fileSize = byteBuf.readLong();
                System.out.println("Длинна файла: " + fileSize);
                currentState = State.FILE_READ;
            }

            if (currentState == State.FILE_READ) {
                while (byteBuf.readableBytes() > 0) {
                    outFile.write(byteBuf.readByte());
                    beRead++;
                    if (beRead == fileSize) {
                        System.out.println("Файл прочитан");
                        outFile.flush();
                        outFile.close();
                        currentState = State.UPDATE_FILE_LIST;
                    }
                }
            }

            if (currentState == State.UPDATE_FILE_LIST) {
                builder = new StringBuilder();
                List<FileInfo> serverList = Files.list(userPath)
                        .map(FileInfo::new)
                        .collect(Collectors.toList());
                for (FileInfo fileInfo: serverList) {
                    builder.append(String.format("%s,%s,%d,%s\n",
                            fileInfo.getType(),
                            fileInfo.getFileName(),
                            fileInfo.getSize(),
                            fileInfo.getLastModified()));
                }
                fileService.sendCommand(ctx.channel(), "/fileList\n" + builder.toString());
                currentState = State.WAIT;
            }
        }
    }
}
