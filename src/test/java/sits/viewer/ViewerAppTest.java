package sits.viewer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import javafx.application.Application;

class ViewerAppTest {

    @Test
    void viewerApp_extendsApplication() {
        assertThat(Application.class).isAssignableFrom(ViewerApp.class);
    }
}
