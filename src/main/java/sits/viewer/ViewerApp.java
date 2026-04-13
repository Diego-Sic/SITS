package sits.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ViewerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("V-SITS Viewer");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
