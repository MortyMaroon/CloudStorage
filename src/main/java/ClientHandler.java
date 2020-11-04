import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            while (true) {
                String command = in.readUTF();
                if (command.equals("upload")) {
                    try {
                        File file = new File("server/" + in.readUTF());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        long size = in.readLong();
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buffer = new byte[256];
                        for (int i = 0; i < (size + 255) / 256; i++) {
                            int read = in.read(buffer);
                            fos.write(buffer,0,read);
                        }
                        fos.close();
                        out.writeUTF("OK");
                    } catch (Exception e) {
                        out.writeUTF("WRONG");
                    }
                }
                if (command.equals("download")) {
                    // TODO: 30.10.2020
                    try {
                        File file = new File("server/" + in.readUTF());
                        long length = file.length();
                        out.writeLong(length);
                        FileInputStream fis = new FileInputStream(file);
                        int read;
                        byte[] buffer = new byte[256];
                        while ((read = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        out.writeUTF("OK");
                    } catch (Exception e) {
                        out.writeUTF("WRONG");
                    }
                }
                if (command.equals("exit")) {
                    System.out.println("Client disconnect corrected");
                    out.writeUTF("OK");
                    break;
                }
                System.out.println(command);
            }
        } catch (SocketException socketException) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
