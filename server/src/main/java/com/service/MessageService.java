package com.service;

import com.utils.Signal;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class MessageService {
    private ByteBuf buffer;

    public void sendCommand(Channel channel, String command) {
        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + command.length());
        buffer.writeByte(Signal.COMMAND);
        buffer.writeInt(command.length());
        buffer.writeBytes(command.getBytes());
        channel.writeAndFlush(buffer);
    }

    public void uploadFile(Channel channel, Path path , String userPath) throws IOException {
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(new File(String.valueOf(path))));
        long fileSize = Files.size(path);
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        byte[] userPathByte = userPath.getBytes(StandardCharsets.UTF_8);

        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + userPathByte.length + 4 + filenameBytes.length + 8 + (int) fileSize);
        buffer.writeByte(Signal.FILE);
        buffer.writeInt(userPath.length());
        buffer.writeBytes(userPathByte);
        buffer.writeInt(path.getFileName().toString().length());
        buffer.writeBytes(filenameBytes);
        buffer.writeLong(Files.size(path));

        int read;
        byte[] buf = new byte[256];
        while ((read = fileIn.read(buf)) != -1) {
            buffer.writeBytes(buf,0,read);
        }
        channel.writeAndFlush(buffer);

        fileIn.close();
    }
}
