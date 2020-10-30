import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Client() throws HeadlessException, IOException {
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setSize(300,300);
        JPanel panel = new JPanel(new GridLayout(2,1));
        JButton send = new JButton("SEND");
        JTextField text = new JTextField();
        send.addActionListener(a -> {
            String[] cmd = text.getText().split(" ");
            if (cmd[0].equals("upload")) {
                sendFile(cmd[1]);
            }
            if (cmd[0].equals("download")) {
                getFile(cmd[1]);
            }
        });
        panel.add(text);
        panel.add(send);
        add(panel);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                sendMassage("exit");
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void getFile(String fileName) {
        // TODO: 30.10.2020
    }

    private void sendFile(String fileName) {
        try {
            out.writeUTF("upload");
            out.writeUTF(fileName);
            File file = new File("/client" + fileName);
            long length = file.length();
            out.writeLong(length);
            FileInputStream fileBytes = new FileInputStream(file);
            int read;
            byte[] buffer = new byte[256];
            while ((read = fileBytes.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            String status = in.readUTF();
            System.out.println(status);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMassage(String text) {
        try {
            out.writeUTF(text);
            System.out.println(in.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
        new Client();
    }
}
