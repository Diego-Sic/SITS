package sits.viewer;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import sits.replay.ReplayPlayer;
import sits.replay.ReplayState;

@ExtendWith(ApplicationExtension.class)
class ReplayControllerTest {

    private ReplayController controller;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/replay.fxml"));
        stage.setScene(new Scene(loader.load()));
        controller = loader.getController();
        stage.show();
    }

    private ReplayPlayer mockPlayer(int commandCount) {
        ReplayPlayer player = mock(ReplayPlayer.class);
        when(player.getState()).thenReturn(new ReplayState());
        when(player.getCommandCount()).thenReturn(commandCount);
        when(player.getNextIndex()).thenReturn(0);
        return player;
    }

    @Test
    void controlsAreDisabledBeforePlayerLoaded(FxRobot robot) {
        assertThat(robot.lookup("#playPauseButton").queryAs(Button.class).isDisabled()).isTrue();
        assertThat(robot.lookup("#stepBackButton").queryAs(Button.class).isDisabled()).isTrue();
        assertThat(robot.lookup("#stepForwardButton").queryAs(Button.class).isDisabled()).isTrue();
        assertThat(robot.lookup("#scrubBar").queryAs(Slider.class).isDisabled()).isTrue();
    }

    @Test
    void setPlayerEnablesAllControls(FxRobot robot) {
        robot.interact(() -> controller.setPlayer(mockPlayer(4)));

        assertThat(robot.lookup("#playPauseButton").queryAs(Button.class).isDisabled()).isFalse();
        assertThat(robot.lookup("#stepBackButton").queryAs(Button.class).isDisabled()).isFalse();
        assertThat(robot.lookup("#stepForwardButton").queryAs(Button.class).isDisabled()).isFalse();
        assertThat(robot.lookup("#scrubBar").queryAs(Slider.class).isDisabled()).isFalse();
    }

    @Test
    void setPlayerSetsSliderMax(FxRobot robot) {
        robot.interact(() -> controller.setPlayer(mockPlayer(7)));

        Slider scrubBar = robot.lookup("#scrubBar").queryAs(Slider.class);
        assertThat(scrubBar.getMax()).isEqualTo(7.0);
    }

    @Test
    void togglePlayPause_changesButtonTextToPause(FxRobot robot) {
        ReplayPlayer player = mockPlayer(4);
        when(player.playAsync(anyLong(), any())).thenReturn(new CompletableFuture<>());

        robot.interact(() -> controller.setPlayer(player));
        robot.interact(() -> controller.togglePlayPause());

        Button btn = robot.lookup("#playPauseButton").queryAs(Button.class);
        assertThat(btn.getText()).isEqualTo("Pause");
    }

    @Test
    void togglePlayPause_secondCallPausesAndRestoresPlayText(FxRobot robot) {
        ReplayPlayer player = mockPlayer(4);
        when(player.playAsync(anyLong(), any())).thenReturn(new CompletableFuture<>());

        robot.interact(() -> controller.setPlayer(player));
        robot.interact(() -> controller.togglePlayPause());
        robot.interact(() -> controller.togglePlayPause());

        Button btn = robot.lookup("#playPauseButton").queryAs(Button.class);
        assertThat(btn.getText()).isEqualTo("Play");
        verify(player).pause();
    }

    @Test
    void stepBack_delegatesToPlayer(FxRobot robot) {
        ReplayPlayer player = mockPlayer(4);

        robot.interact(() -> controller.setPlayer(player));
        robot.interact(() -> controller.stepBack());

        verify(player).stepBack();
    }

    @Test
    void stepForward_delegatesToPlayer(FxRobot robot) {
        ReplayPlayer player = mockPlayer(4);

        robot.interact(() -> controller.setPlayer(player));
        robot.interact(() -> controller.stepForward());

        verify(player).stepForward();
    }

    @Test
    void stepBack_isNoOpBeforePlayerLoaded(FxRobot robot) {
        // Should not throw when no player is set
        robot.interact(() -> controller.stepBack());
    }

    @Test
    void stepForward_isNoOpBeforePlayerLoaded(FxRobot robot) {
        robot.interact(() -> controller.stepForward());
    }

    @Test
    void togglePlayPause_isNoOpBeforePlayerLoaded(FxRobot robot) {
        robot.interact(() -> controller.togglePlayPause());
    }

    @Test
    void setPlayerRendersRoundLabelFromState(FxRobot robot) {
        ReplayPlayer player = mock(ReplayPlayer.class);
        ReplayState state = new ReplayState();
        // advance state to round 3 manually so we can check renderUI reads from state
        state.setCurrentRound(3);
        when(player.getState()).thenReturn(state);
        when(player.getCommandCount()).thenReturn(4);
        when(player.getNextIndex()).thenReturn(0);

        robot.interact(() -> controller.setPlayer(player));

        assertThat(robot.lookup("#roundLabel").queryLabeled().getText()).isEqualTo("Round: 3");
    }
}
