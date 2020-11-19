package com.client;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Network {
    private static Network instanceNetwork;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ByteBuffer buffer;

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
            int i = in.readInt();
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                builder.append((char)in.readByte());
            }
            return builder.toString();

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
        if (socket != null) {
            return true;
        } else {
            return false;
        }
    }
}

