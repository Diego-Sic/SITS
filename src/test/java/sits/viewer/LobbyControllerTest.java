package sits.viewer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
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
}
