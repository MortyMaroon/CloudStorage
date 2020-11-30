package com.handlers;

import com.service.AuthService;
import com.utils.FileInfo;
import com.service.MessageService;
import com.utils.FileService;
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
    private long wasRead = 0L;
    private BufferedOutputStream outFile;
    private StringBuilder builder;
    private String userName;
    private Path userPath;
    private final MessageService messageService = new MessageService();
    private final FileService fileService = new FileService();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
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
                    currentState = State.FILE_NAME_LENGTH;
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
                currentState = State.COMMAND_DOING;
            }

            if (currentState == State.COMMAND_DOING) {
                System.out.println(builder.toString());
                String[] cmd = builder.toString().split("\n");
                switch (cmd[0]) {

                    case "/auth":
                        Path path = AuthService.authorization(cmd[1],cmd[2]);
                        if (path != null) {
                            userName = cmd[1];
                            userPath = path;
                            messageService.sendCommand(ctx.channel(), "/auth\nok");
                        } else {
                            messageService.sendCommand(ctx.channel(), "/auth\nnoSuch");
                        }
                        currentState = State.WAIT;
                        break;

                    case "/reg":
                        Path newPath = AuthService.registration(cmd[1],cmd[2]);
                        if (newPath != null) {
                            userName = cmd[1];
                            userPath = newPath;
                            messageService.sendCommand(ctx.channel(), "/auth\nok");
                        } else {
                            messageService.sendCommand(ctx.channel(), "/login\nbusy");
                        }
                        currentState = State.WAIT;
                        break;

                    case "exit":
                        messageService.sendCommand(ctx.channel(), "exit\nOk");
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
                        if (userPath.equals(Path.of(AuthService.getRootPath(), userName))){
                            currentState = State.WAIT;
                        } else {
                            userPath = userPath.getParent();
                            currentState = State.UPDATE_FILE_LIST;
                        }
                        break;

                    case "/delete":
                        try {
                            fileService.delete(userPath.resolve(cmd[1]));
                            currentState = State.UPDATE_FILE_LIST;
                            break;
                        } catch (IOException exception) {
                            messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getMessage()));
                            currentState = State.WAIT;
                            break;
                        }

                    case "/download":
                        try {
                            messageService.uploadFile(ctx.channel(), userPath.resolve(cmd[1]), cmd[2]);
                            currentState = State.WAIT;
                            break;
                        } catch (IOException exception) {
                            messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getMessage()));
                            currentState = State.WAIT;
                            break;
                        }

                    case "/disconnect":
                        userName = null;
                        userPath = null;
                        messageService.sendCommand(ctx.channel(), "/disconnect\nOk");
                        currentState = State.WAIT;
                        break;

                    case "/mkdir":
                        try {
                            fileService.createDirectory(userPath, cmd[1]);
                            currentState = State.UPDATE_FILE_LIST;
                            break;
                        } catch (Exception exception) {
                            messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getCause().getMessage()));
                            currentState = State.WAIT;
                            break;
                        }

                    case "/rename":
                        try {
                            fileService.renameFile(userPath, cmd[1], cmd[2]);
                            currentState = State.UPDATE_FILE_LIST;
                            break;
                        } catch (Exception exception) {
                            messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getCause().getMessage()));
                            currentState = State.WAIT;
                            break;
                        }
                    case "/createFile":
                        try {
                            fileService.createFile(userPath, cmd[1]);
                            currentState = State.UPDATE_FILE_LIST;
                            break;
                        } catch (Exception exception) {
                            messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getCause().getMessage()));
                            currentState = State.WAIT;
                            break;
                        }
                    case "/toHomeDirectory":
                        userPath = Path.of(AuthService.getRootPath(), userName);
                        currentState = State.UPDATE_FILE_LIST;
                        break;

                    default:
                        currentState = State.WAIT;
                        throw new IllegalAccessException("Unknown command: " + builder.toString());
                }
            }

            if (currentState == State.FILE_NAME_LENGTH) {
                wasRead = 0L;
                fileSize = 0L;
                System.out.println("Get filename length.");
                filenameLength = byteBuf.readInt();
                currentState = State.FILE_NAME;
            }

            if (currentState == State.FILE_NAME) {
                byte[] filenameInBytes = new byte[filenameLength];
                byteBuf.readBytes(filenameInBytes);
                String filename = new String(filenameInBytes, StandardCharsets.UTF_8);
                File downloadFile = new File(userPath.toString() + File.separator + filename);
                System.out.println("Filename received: " + filename);
                try {
                    outFile = new BufferedOutputStream(new FileOutputStream(downloadFile));
                } catch (FileNotFoundException exception) {
                    messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getCause().getMessage()));
                    currentState = State.WAIT;
                    break;
                }
                currentState = State.FILE_LENGTH;
            }

            if (currentState == State.FILE_LENGTH) {
                fileSize = byteBuf.readLong();
                System.out.println("File size received: " + fileSize);
                currentState = State.FILE_READ;
            }

            if (currentState == State.FILE_READ) {
                while (byteBuf.readableBytes() > 0) {
                    outFile.write(byteBuf.readByte());
                    wasRead++;
                    if (wasRead == fileSize) {
                        outFile.flush();
                        outFile.close();
                        System.out.println("File received");
                        currentState = State.UPDATE_FILE_LIST;
                        break;
                    }
                }
            }

            if (currentState == State.UPDATE_FILE_LIST) {
                try {
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
                    messageService.sendCommand(ctx.channel(), "/fileList\n" + builder.toString());
                    messageService.sendCommand(ctx.channel(), "/serverPath\n" + userPath.normalize().toString());
                    currentState = State.WAIT;
                } catch (IOException exception) {
                    messageService.sendCommand(ctx.channel(), String.format("/error\n%s\n%s", exception.getClass().getSimpleName(), exception.getMessage()));
                    currentState = State.WAIT;
                    break;
                }
            }
        }
    }
}
