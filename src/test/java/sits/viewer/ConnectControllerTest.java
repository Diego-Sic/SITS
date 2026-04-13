package sits.viewer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

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
}
