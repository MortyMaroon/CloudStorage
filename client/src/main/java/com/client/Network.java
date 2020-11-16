package com.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Network instanceNetwork;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

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
            byte[] buffer = new byte[1024];
            int count = in.read(buffer);
            return new String(buffer,0,count);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMassage(String massage) {
        try {
            out.write(massage.getBytes());
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

