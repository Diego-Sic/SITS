package sits.viewer;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class LobbyControllerTest {

        private LobbyController controller;

        @Start
        void start(Stage stage) throws Exception {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
                stage.setScene(new Scene(loader.load()));
                controller = loader.getController();
                stage.show();
        }

        @Test
        void watchButton_isDisabledByDefault(FxRobot robot) {
                Button watchButton = robot.lookup("#watchButton").queryAs(Button.class);
                assertThat(watchButton.isDisabled()).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void watchButton_enablesWhenRunningTournamentSelected(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t1\",\"name\":\"IPD Tournament\",\"status\":\"RUNNING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> mockResponse = mock(HttpResponse.class);
                when(mockResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(mockResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));

                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                Button watchButton = robot.lookup("#watchButton").queryAs(Button.class);
                assertThat(watchButton.isDisabled()).isFalse();
        }

        @Test
        void startButton_isDisabledByDefault(FxRobot robot) {
                Button startButton = robot.lookup("#startButton").queryAs(Button.class);
                assertThat(startButton.isDisabled()).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void startButton_enablesWhenRegisteringTournamentSelected(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t2\",\"name\":\"New Tournament\",\"status\":\"REGISTERING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> mockResponse = mock(HttpResponse.class);
                when(mockResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(mockResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));

                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                Button startButton = robot.lookup("#startButton").queryAs(Button.class);
                assertThat(startButton.isDisabled()).isFalse();
        }

        @Test
        @SuppressWarnings("unchecked")
        void watchButton_staysDisabledWhenRegisteringTournamentSelected(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t2\",\"name\":\"Test Tournament\",\"status\":\"REGISTERING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> mockResponse = mock(HttpResponse.class);
                when(mockResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(mockResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));

                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                Button watchButton = robot.lookup("#watchButton").queryAs(Button.class);
                assertThat(watchButton.isDisabled()).isTrue();
        }

        // startSelected tests

        @Test
        @SuppressWarnings("unchecked")
        void startSelected_nullSelection_isNoOp(FxRobot robot) throws Exception {
                // Empty list so nothing is selectable
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> mockResponse = mock(HttpResponse.class);
                when(mockResponse.body()).thenReturn("[]");
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(mockResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));
                // init's refresh = 1 send; after that, client should be untouched if
                // startSelected no-ops
                robot.interact(() -> controller.startSelected());

                verify(mockClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void startSelected_success_callsStartAndRefreshes(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t1\",\"name\":\"T\",\"status\":\"REGISTERING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> listResponse = mock(HttpResponse.class);
                when(listResponse.body()).thenReturn(json);
                HttpResponse<Void> voidResponse = mock(HttpResponse.class);
                // 3 sends expected: init-refresh, startTournament POST, post-start refresh.
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(listResponse)
                                .thenReturn(voidResponse)
                                .thenReturn(listResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));
                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                robot.interact(() -> controller.startSelected());

                verify(mockClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void startSelected_exception_showsAlert(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t1\",\"name\":\"T\",\"status\":\"REGISTERING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> listResponse = mock(HttpResponse.class);
                when(listResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(listResponse)
                                .thenThrow(new IOException("boom"));
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));
                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                // Schedule the handler on the FX thread so showAndWait can block there
                // while the test thread waits for the alert window to appear.
                Platform.runLater(() -> controller.startSelected());

                WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                                () -> robot.lookup(".dialog-pane").tryQuery().isPresent());

                DialogPane pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
                assertThat(pane.getHeaderText()).isEqualTo("Failed to start tournament");

                // Dismiss so the FX thread un-blocks and the next test starts clean
                robot.push(KeyCode.ENTER);
                WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @SuppressWarnings("unchecked")
        void watchSelected_nullSelection_isNoOp(FxRobot robot) throws Exception {
                // Populate list with a RUNNING tournament but deliberately do not select it
                String json = "[{\"id\":\"t1\",\"name\":\"T\",\"status\":\"RUNNING\"}]";
                HttpClient mockClient = mock(HttpClient.class);
                HttpResponse<String> listResponse = mock(HttpResponse.class);
                when(listResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(listResponse);
                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));
                robot.interact(() -> controller.watchSelected());

                // Lobby scene should still be active — no transition happened
                assertThat(robot.lookup("#tournamentList").tryQuery()).isPresent();
                assertThat(robot.lookup("#feedArea").tryQuery()).isNotPresent();
        }

        @Test
        @SuppressWarnings("unchecked")
        void watchSelected_success_transitionsToLiveGame(FxRobot robot) throws Exception {
                String json = "[{\"id\":\"t1\",\"name\":\"T\",\"status\":\"RUNNING\"}]";
                HttpClient mockClient = mock(HttpClient.class);

                // fetchTournaments (sync)
                HttpResponse<String> listResponse = mock(HttpResponse.class);
                when(listResponse.body()).thenReturn(json);
                when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(listResponse);

                // streamMoves (async) — empty stream so onDone fires immediately
                HttpResponse<Stream<String>> streamResponse = mock(HttpResponse.class);
                when(streamResponse.body()).thenReturn(Stream.of());
                when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(CompletableFuture.completedFuture(streamResponse));

                ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                                new ObjectMapper(), Runnable::run);

                robot.interact(() -> controller.init(conn));
                ListView<TournamentInfo> list = robot.lookup("#tournamentList").queryListView();
                robot.interact(() -> list.getSelectionModel().select(0));

                robot.interact(() -> controller.watchSelected());

                // We change of view so live_game is now active, lobby is gone
                assertThat(robot.lookup("#feedArea").tryQuery()).isPresent();
                assertThat(robot.lookup("#tournamentList").tryQuery()).isNotPresent();
        }
}
