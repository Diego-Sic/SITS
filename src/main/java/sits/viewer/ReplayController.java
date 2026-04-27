package sits.viewer;

import java.io.IOException;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import sits.replay.MoveEntry;
import sits.replay.ReplayFile;
import sits.replay.ReplayPlayer;

public class ReplayController {

    @FXML private ListView<String> replayList;
    @FXML private Slider scrubBar;
    @FXML private Button playPauseButton;
    @FXML private Button stepBackButton;
    @FXML private Button stepForwardButton;
    @FXML private Label roundLabel;
    @FXML private TextArea moveLogArea;
    @FXML private TextArea scoreArea;

    private ServerConnection connection;
    private ReplayPlayer player;
    private boolean playing = false;
    private boolean updatingSlider = false;

    public void init(ServerConnection connection) {
        this.connection = connection;
        scrubBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingSlider && player != null) {
                player.seekTo(newVal.intValue());
                renderUI();
            }
        });
        loadReplayList();
    }

    // Package-private — allows tests to inject a player directly without HTTP
    void setPlayer(ReplayPlayer player) {
        this.player = player;
        scrubBar.setMax(player.getCommandCount());
        enableControls();
        renderUI();
    }

    private void loadReplayList() {
        try {
            List<ReplayFile.Meta> metas = connection.fetchReplays();
            replayList.getItems().setAll(
                    metas.stream().map(ReplayFile.Meta::tournamentId).toList());
        } catch (Exception e) {
            showError("Failed to load replays", e.getMessage());
        }
    }

    @FXML
    public void onReplaySelected() {
        String id = replayList.getSelectionModel().getSelectedItem();
        if (id == null) return;
        try {
            ReplayFile file = connection.fetchReplayFile(id);
            setPlayer(new ReplayPlayer(file.getCommands()));
        } catch (Exception e) {
            showError("Failed to load replay", e.getMessage());
        }
    }

    @FXML
    public void stepBack() {
        if (player == null) return;
        player.stepBack();
        renderUI();
    }

    @FXML
    public void stepForward() {
        if (player == null) return;
        player.stepForward();
        renderUI();
    }

    @FXML
    public void togglePlayPause() {
        if (player == null) return;
        if (playing) {
            player.pause();
            playing = false;
            playPauseButton.setText("Play");
        } else {
            playing = true;
            playPauseButton.setText("Pause");
            player.playAsync(500, () -> Platform.runLater(this::renderUI))
                  .thenRun(() -> Platform.runLater(() -> {
                      playing = false;
                      playPauseButton.setText("Play");
                  }));
        }
    }

    @FXML
    public void back() {
        if (player != null && playing) player.pause();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobby = loader.getController();
            lobby.init(connection);
            Stage stage = (Stage) replayList.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Navigation Error", e.getMessage());
        }
    }

    private void renderUI() {
        if (player == null) return;
        roundLabel.setText("Round: " + player.getState().getCurrentRound());
        updatingSlider = true;
        scrubBar.setValue(player.getNextIndex());
        updatingSlider = false;
        renderMoveLog();
        renderScores();
    }

    private void renderMoveLog() {
        StringBuilder sb = new StringBuilder();
        for (MoveEntry e : player.getState().getMoveLog()) {
            sb.append("R").append(e.roundNumber())
              .append(": ").append(e.nameP1()).append(" ").append(e.actionP1())
              .append(" | ").append(e.nameP2()).append(" ").append(e.actionP2())
              .append(" (").append(e.payoffP1()).append(", ").append(e.payoffP2()).append(")\n");
        }
        moveLogArea.setText(sb.toString());
    }

    private void renderScores() {
        StringBuilder sb = new StringBuilder();
        player.getState().getScores()
              .forEach((name, score) -> sb.append(name).append(": ").append(score).append("\n"));
        scoreArea.setText(sb.toString());
    }

    private void enableControls() {
        stepBackButton.setDisable(false);
        stepForwardButton.setDisable(false);
        playPauseButton.setDisable(false);
        scrubBar.setDisable(false);
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
