import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    final String IP = "localhost";
    final int PORT = 8189;

    @FXML
    private TextField Login, NewLogin;

    @FXML
    private PasswordField Password, NewPassword, ConfPassword;

    @FXML
    private Label SignInMessage, SignUpMessage;

    @FXML
    private Pane pnlReg, pnlSignIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connect();
    }

    private void connect() {
        try {
            socket = new Socket(IP, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authorization();
//                    read();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
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

    private void authorization() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/authOk")) {
                // TODO: 04.11.2020
                break;
            }
            if (str.startsWith("/busy")) SignInMessage.setText("This user is online.");
            if (str.startsWith("/noSuch")) SignInMessage.setText("Invalid Login. Please try again.");
            if (str.startsWith("/loginNO")) SignUpMessage.setText("Login is busy.");
            if (str.startsWith("/nickNO")) SignUpMessage.setText("Nickname is busy.");
        }
    }

    public void signIN() {
        if (!Login.getText().isEmpty() && !Password.getText().isEmpty()) {
            sendMassage("/auth " + Login.getText() + " " + Password.getText());
            Login.clear();
            Password.clear();
        } else {
            SignInMessage.setText("Please enter login and password.");
        }
    }

    public void signUp() {
        SignUpMessage.setText("");
        if (!NewPassword.getText().isEmpty() && !ConfPassword.getText().isEmpty() && !NewLogin.getText().isEmpty()) {
            if (!NewPassword.getText().equals(ConfPassword.getText())) {
                SignUpMessage.setText("Passwords do not match.");
            } else {
                sendMassage("/checkLogin" + Login.getText() + " " + Password.getText());
            }
        } else {
            SignUpMessage.setText("Please enter all data.");
        }
        NewPassword.clear();
        ConfPassword.clear();
    }

    public void changeSignUpMenu() {
        pnlReg.toFront();
        pnlReg.setVisible(true);
        pnlReg.setDisable(false);
        pnlSignIn.setDisable(true);
        pnlSignIn.setVisible(false);
    }

    public void changeSignInMenu() {
        pnlSignIn.toFront();
        pnlSignIn.setVisible(true);
        pnlSignIn.setDisable(false);
        pnlReg.setDisable(true);
        pnlReg.setVisible(false);
    }

    private void sendMassage(String massage) {
        try {
            out.writeUTF(massage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exit() {
        if (socket != null){
            try {
                out.writeUTF("/end");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
