package sits.viewer;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class LobbyController {

    @FXML private ListView<TournamentInfo> tournamentList;
    @FXML private Button watchButton;
    @FXML private Button startButton;
    private ServerConnection connection;

    public void init(ServerConnection connection) {
        this.connection = connection;
        watchButton.setDisable(true);
        startButton.setDisable(true);
        tournamentList.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                boolean isRunning = sel != null && "RUNNING".equals(sel.status);
                boolean isRegistering = sel != null && "REGISTERING".equals(sel.status);
                watchButton.setDisable(!isRunning);
                startButton.setDisable(!isRegistering);
            });
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<TournamentInfo> tournaments = connection.fetchTournaments();
            tournamentList.getItems().setAll(tournaments);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Failed to fetch tournaments");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void startSelected() {
        TournamentInfo selected = tournamentList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            connection.startTournament(selected.id);
            refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to start tournament");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void watchSelected() {
        TournamentInfo selected = tournamentList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/live_game.fxml"));
            Parent root = loader.load();
            LiveGameController liveGame = loader.getController();
            liveGame.init(connection, selected.id);
            liveGame.startStream();
            Stage stage = (Stage) tournamentList.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load game view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
