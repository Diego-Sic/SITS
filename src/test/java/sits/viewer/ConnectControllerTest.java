package sits.viewer;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.sun.net.httpserver.HttpServer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class ConnectControllerTest {

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @Test
    void connect_emptyFields_showsError(FxRobot robot) {
        // Fields are empty by default — click Connect immediately
        robot.clickOn("#connectButton");

        Label error = robot.lookup("#errorLabel").queryAs(Label.class);
        assertThat(error.getText()).isNotEmpty();
    }

    @Test
    void connect_filledFields_clearsValidationError(FxRobot robot) {
        robot.clickOn("#ipField").write("127.0.0.1");
        robot.clickOn("#portField").write("8080");

        // Validation error should not appear before clicking Connect
        Label error = robot.lookup("#errorLabel").queryAs(Label.class);
        assertThat(error.getText()).isEmpty();
    }

    @Test
    void connect_filledFields_transitionsToLobby(FxRobot robot) throws Exception {
        // Spin up a tiny in-process HTTP server so the lobby's init-refresh gets a
        // valid empty-tournament list instead of a connection refused. Without this,
        // refresh() would throw, show an Alert, and block the FX thread.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tournaments", exchange -> {
            byte[] body = "[]".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();

        try {
            robot.clickOn("#ipField").write("127.0.0.1");
            robot.clickOn("#portField").write(String.valueOf(port));
            robot.clickOn("#connectButton");
            WaitForAsyncUtils.waitForFxEvents();

            // Scene transitioned to lobby: #tournamentList present, #ipField gone
            assertThat(robot.lookup("#tournamentList").tryQuery()).isPresent();
            assertThat(robot.lookup("#ipField").tryQuery()).isNotPresent();
        } finally {
            server.stop(0);
        }
    }
}
