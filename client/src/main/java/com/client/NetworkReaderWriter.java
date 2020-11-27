package com.client;

import com.utils.Signal;

import java.io.*;
import java.nio.ByteBuffer;

public class NetworkReaderWriter {

    public void writeToNetwork(DataOutputStream out, String command) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1+4+command.length());
        buffer.put(Signal.COMMAND);
        buffer.putInt(command.length());
        buffer.put(command.getBytes());
        out.write(buffer.array());
    }

    public String readFromNetwork(DataInputStream inputStream) throws IOException {
        byte signal = inputStream.readByte();
        if (signal == Signal.COMMAND) {
            int i = inputStream.readInt();
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                builder.append((char) inputStream.readByte());
            }
            return builder.toString();
        }
        if (signal == Signal.FILE) {
            FileService fileService = new FileService();
            fileService.getFile(inputStream);
            return "/updateUserList";
        }
        return "Unknown command";
    }
}
