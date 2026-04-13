package sits.viewer;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConnectController {

    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private Label errorLabel;

    @FXML
    public void connect() {
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();

        if (ip.isEmpty() || port.isEmpty()) {
            errorLabel.setText("Please enter an IP address and port.");
            return;
        }

        String baseUrl = "http://" + ip + ":" + port;
        ServerConnection connection = new ServerConnection(baseUrl);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobby = loader.getController();
            lobby.init(connection);

            Stage stage = (Stage) ipField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            errorLabel.setText("Failed to load lobby screen.");
        }
    }
}
