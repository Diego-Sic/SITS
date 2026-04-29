package sits.replay.commands;

import org.junit.jupiter.api.Test;
import sits.replay.GameSummary;
import sits.replay.ReplayState;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameEndCommandTest {

    private static final GameSummary SUMMARY = new GameSummary("Alice", "Alice", "Bob", 9, 6);

    @Test
    void applyAppendsToCompletedGames() {
        ReplayState state = new ReplayState();
        new GameEndCommand(SUMMARY).apply(state);
        assertThat(state.getCompletedGames()).containsExactly(SUMMARY);
    }

    @Test
    void applyDoesNotChangeOtherFields() {
        ReplayState state = new ReplayState();
        new GameEndCommand(SUMMARY).apply(state);
        assertThat(state.getCurrentRound()).isZero();
        assertThat(state.getScores()).isEmpty();
        assertThat(state.getMoveLog()).isEmpty();
        assertThat(state.getFinalResult()).isEmpty();
    }

    @Test
    void undoRemovesLastCompletedGame() {
        ReplayState state = new ReplayState();
        GameEndCommand cmd = new GameEndCommand(SUMMARY);
        cmd.apply(state);
        cmd.undo(state);
        assertThat(state.getCompletedGames()).isEmpty();
    }

    @Test
    void applyThenUndoIsIdenticalToPreApply() {
        ReplayState state = new ReplayState();
        List<GameSummary> gamesBefore = new ArrayList<>(state.getCompletedGames());

        GameEndCommand cmd = new GameEndCommand(SUMMARY);
        cmd.apply(state);
        cmd.undo(state);

        assertThat(state.getCompletedGames()).isEqualTo(gamesBefore);
    }

    @Test
    void applyThenUndoIsIdenticalWithExistingGame() {
        ReplayState state = new ReplayState();
        GameSummary first = new GameSummary("Bob", "Alice", "Bob", 6, 9);
        new GameEndCommand(first).apply(state);

        List<GameSummary> gamesBefore = new ArrayList<>(state.getCompletedGames());

        GameEndCommand cmd = new GameEndCommand(SUMMARY);
        cmd.apply(state);
        cmd.undo(state);

        assertThat(state.getCompletedGames()).isEqualTo(gamesBefore);
    }
}
