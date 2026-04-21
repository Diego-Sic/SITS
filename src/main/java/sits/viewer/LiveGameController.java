package sits.viewer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import sits.networking.dto.MoveEventDTO;

public class LiveGameController {

    @FXML
    private TextArea feedArea;
    private ServerConnection connection;
    private String tournamentId;
    private CompletableFuture<Void> stream;

    public void init(ServerConnection connection, String tournamentId) {
        this.connection = connection;
        this.tournamentId = tournamentId;
    }

    @FXML
    public void initialize() {
        // startStream() is called externally by LobbyController after init(), cool,
        // right?
    }

    public void startStream() {
        stream = connection.streamMoves(tournamentId,
                this::appendEvent,
                () -> feedArea.appendText("Stream closed.\n"));
    }

    private void appendEvent(MoveEventDTO dto) {
        switch (dto.type) {
            case "MOVE" -> feedArea.appendText(
                    "Round " + dto.roundNumber + ": " + dto.p1Name +
                            " played " + dto.action1 + " | " + dto.p2Name +
                            " played " + dto.action2 + "\n");
            case "GAME_OVER" -> feedArea.appendText(
                    "--- Game over. Winner: " + dto.winner + " ---\n");
            case "TOURNAMENT_OVER" -> feedArea.appendText(
                    "=== Tournament complete ===\n");
        }
    }

    @FXML
    public void back() {
        if (stream != null)
            stream.cancel(true);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobby = loader.getController();
            lobby.init(connection);
            Stage stage = (Stage) feedArea.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            feedArea.appendText("Failed to navigate back.\n");
        }
    }
}
