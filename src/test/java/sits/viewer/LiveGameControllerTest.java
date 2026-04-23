package sits.viewer;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class LiveGameControllerTest {

    private LiveGameController controller;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/live_game.fxml"));
        stage.setScene(new Scene(loader.load()));
        controller = loader.getController();
        stage.show();
    }

    @Test
    void feedArea_isInitiallyEmpty(FxRobot robot) {
        TextArea feedArea = robot.lookup("#feedArea").queryAs(TextArea.class);
        assertThat(feedArea.getText()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void startStream_moveEvent_appendsRoundText(FxRobot robot) {
        String json = "{\"type\":\"MOVE\",\"roundNumber\":1,\"p1Name\":\"Alice\",\"p2Name\":\"Bob\"," +
                "\"action1\":\"COOPERATE\",\"action2\":\"DEFECT\",\"payoff1\":3,\"payoff2\":0}";
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<Stream<String>> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(Stream.of("data: " + json));
        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                new ObjectMapper(), Runnable::run);

        robot.interact(() -> {
            controller.init(conn, "t1");
            controller.startStream();
        });

        TextArea feedArea = robot.lookup("#feedArea").queryAs(TextArea.class);
        assertThat(feedArea.getText()).contains("Round 1: Alice played COOPERATE | Bob played DEFECT");
    }

    @Test
    @SuppressWarnings("unchecked")
    void startStream_gameOverEvent_appendsWinnerText(FxRobot robot) {
        String json = "{\"type\":\"GAME_OVER\",\"winner\":\"Alice\",\"roundNumber\":10," +
                "\"p1Name\":\"Alice\",\"p2Name\":\"Bob\",\"action1\":null,\"action2\":null," +
                "\"payoff1\":0,\"payoff2\":0,\"totalScore1\":30,\"totalScore2\":10}";
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<Stream<String>> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(Stream.of("data: " + json));
        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                new ObjectMapper(), Runnable::run);

        robot.interact(() -> {
            controller.init(conn, "t1");
            controller.startStream();
        });

        TextArea feedArea = robot.lookup("#feedArea").queryAs(TextArea.class);
        assertThat(feedArea.getText()).contains("--- Game over. Winner: Alice ---");
    }

    @Test
    @SuppressWarnings("unchecked")
    void startStream_onDone_appendsStreamClosedText(FxRobot robot) {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<Stream<String>> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(Stream.of());
        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                new ObjectMapper(), Runnable::run);

        robot.interact(() -> {
            controller.init(conn, "t1");
            controller.startStream();
        });

        TextArea feedArea = robot.lookup("#feedArea").queryAs(TextArea.class);
        assertThat(feedArea.getText()).contains("Stream closed.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void back_cancelsActiveStream(FxRobot robot) throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        // Pending future, stream stays "open" so we can observe cancellation ;D
        CompletableFuture<HttpResponse<Stream<String>>> pending = new CompletableFuture<>();
        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(pending);
        // back() triggers lobby.init
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.body()).thenReturn("[]");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(listResponse);

        ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                new ObjectMapper(), Runnable::run);

        robot.interact(() -> {
            controller.init(conn, "t1");
            controller.startStream();
        });

        // Capture the private stream field before back() replaces the scene
        Field streamField = LiveGameController.class.getDeclaredField("stream");
        streamField.setAccessible(true);
        CompletableFuture<?> streamBefore = (CompletableFuture<?>) streamField.get(controller);
        assertThat(streamBefore).isNotNull();
        assertThat(streamBefore.isCancelled()).isFalse();

        robot.interact(() -> controller.back());

        assertThat(streamBefore.isCancelled()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void back_success_transitionsToLobby(FxRobot robot) throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        // lobby.init's refresh call
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.body()).thenReturn("[]");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(listResponse);

        ServerConnection conn = new ServerConnection("http://localhost:8080", mockClient,
                new ObjectMapper(), Runnable::run);

        // No startStream(), just init and back to test scene transition logic in
        // isolation
        robot.interact(() -> controller.init(conn, "t1"));
        robot.interact(() -> controller.back());

        // Scene transitioned: lobby's #tournamentList now present, #feedArea gone
        assertThat(robot.lookup("#tournamentList").tryQuery()).isPresent();
        assertThat(robot.lookup("#feedArea").tryQuery()).isNotPresent();
    }
}
