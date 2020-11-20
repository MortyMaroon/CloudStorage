package com.client;


import com.utils.Signal;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Network {
    private static Network instanceNetwork;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedInputStream fileIn;
    private BufferedOutputStream fileOut;
    private byte[] filePathBytes;
    private byte[] filenameBytes;
    private int filePathLength = 0;
    private int filenameLength = 0;
    private long fileSize = 0L;

    public static Network getNetwork() {
        return instanceNetwork;
    }

    public Network(String ip, int port) {
        try {
            instanceNetwork = this;
            this.socket = new Socket(ip, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public String readMassage() {
        try {
            byte signal = in.readByte();
            if (signal == Signal.COMMAND) {
                int i = in.readInt();
                StringBuilder builder = new StringBuilder();
                for (int j = 0; j < i; j++) {
                    builder.append((char)in.readByte());
                }
                return builder.toString();
            }
            if (signal == Signal.FILE) {
                fileSize = 0L;

                System.out.println("Читаем длинну пути в место сохранения");
                filePathLength = in.readInt();
                System.out.println("Длинну пути в место сохранения равна: " + filePathLength);

                System.out.println("Читаем пути в место сохранения");
                filePathBytes = new byte[filePathLength];
                in.read(filePathBytes, 0, filePathLength);
                String userPath = new String(filePathBytes, StandardCharsets.UTF_8);
                System.out.println("Путь в место сохранения: " + userPath);

                System.out.println("Читаем длинну имени файла");
                filenameLength = in.readInt();
                System.out.println("Длинна имени файла равна: " + filenameLength);

                System.out.println("Читаем имя файла");
                filenameBytes = new byte[filenameLength];
                in.read(filenameBytes, 0 , filenameLength);
                String filename = new String(filenameBytes, StandardCharsets.UTF_8);
                System.out.println("Имя файла: " + filename);

                System.out.println("Настраиваем канал для записи файла");
                File downloadFile = new File(userPath + File.separator + filename);
                fileOut = new BufferedOutputStream(new FileOutputStream(downloadFile));

                System.out.println("Читаем длинну файла");
                fileSize = in.readLong();
                System.out.println("Длинна файла: " + fileSize);

                System.out.println("Читаем файл");
                byte[] buf = new byte[256];
                for (int i = 0; i < (fileSize + 255) / 256; i++) {
                    int read = in.read(buf);
                    fileOut.write(buf,0,read);
                }
                fileOut.flush();
                fileOut.close();
                System.out.println("Файл прочитан");
                return "/updateUserList";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendCommand(String command) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1+4+command.length());
            buffer.put((byte) 20);
            buffer.putInt(command.length());
            buffer.put(command.getBytes());
            out.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(Path path, Long size) throws IOException {
        fileIn = new BufferedInputStream(new FileInputStream(new File(String.valueOf(path))));
        filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        filenameLength = filenameBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + filenameBytes.length + 8 + size.intValue());
        buffer.put(Signal.FILE);
        buffer.putInt(path.getFileName().toString().length());
        buffer.put(filenameBytes);
        buffer.putLong(size);
        int read;
        byte[] buf = new byte[256];
        while ((read = fileIn.read(buf)) != -1) {
            buffer.put(buf, 0, read);
        }
        System.out.println(buffer.array().length);
        out.write(buffer.array());
        fileIn.close();
    }

    public void closeConnection() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getStatus() {
        return socket != null;
    }

}

