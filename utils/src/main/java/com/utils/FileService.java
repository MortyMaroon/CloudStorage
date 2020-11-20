package com.utils;

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


public class FileService {
    private ByteBuf buffer;
    private byte[] filenameBytes;
    private BufferedInputStream fileIn;

    public void sendCommand(Channel channel, String command) {
        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + command.length());
        buffer.writeByte(Signal.COMMAND);
        buffer.writeInt(command.length());
        buffer.writeBytes(command.getBytes());
        channel.writeAndFlush(buffer);
    }

    public void upload(Channel channel, Path path) throws IOException {
        fileIn = new BufferedInputStream(new FileInputStream(new File(String.valueOf(path))));
        Long fileSize = Files.size(path);
        filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8 + fileSize.intValue());
        buffer.writeByte(Signal.FILE);
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
