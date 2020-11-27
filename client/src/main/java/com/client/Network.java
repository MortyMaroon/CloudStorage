package com.client;

import java.io.*;
import java.net.Socket;

public class Network {
    private static Network instanceNetwork;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public static Network getNetwork() {
        return instanceNetwork;
    }

    public DataOutputStream getOutputStream() {
        return out;
    }

    public DataInputStream getInputStream() {
        return in;
    }

    public Network(String ip, int port) throws IOException {
        instanceNetwork = this;
        this.socket = new Socket(ip, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
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

