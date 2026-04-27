package sits.replay.commands;

import org.junit.jupiter.api.Test;
import sits.replay.ReplayState;
import sits.replay.TournamentSummary;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentEndCommandTest {

    private static final TournamentSummary SUMMARY =
            new TournamentSummary(Map.of("Alice", 18, "Bob", 12));

    @Test
    void applySetsFinaResult() {
        ReplayState state = new ReplayState();
        new TournamentEndCommand(SUMMARY).apply(state);
        assertThat(state.getFinalResult()).contains(SUMMARY);
    }

    @Test
    void applyDoesNotChangeOtherFields() {
        ReplayState state = new ReplayState();
        new TournamentEndCommand(SUMMARY).apply(state);
        assertThat(state.getCurrentRound()).isZero();
        assertThat(state.getScores()).isEmpty();
        assertThat(state.getMoveLog()).isEmpty();
        assertThat(state.getCompletedGames()).isEmpty();
    }

    @Test
    void undoClearsFinalResult() {
        ReplayState state = new ReplayState();
        TournamentEndCommand cmd = new TournamentEndCommand(SUMMARY);
        cmd.apply(state);
        cmd.undo(state);
        assertThat(state.getFinalResult()).isEmpty();
    }

    @Test
    void applyThenUndoIsIdenticalToPreApply() {
        ReplayState state = new ReplayState();
        Optional<TournamentSummary> before = state.getFinalResult();

        TournamentEndCommand cmd = new TournamentEndCommand(SUMMARY);
        cmd.apply(state);
        cmd.undo(state);

        assertThat(state.getFinalResult()).isEqualTo(before);
    }
}
