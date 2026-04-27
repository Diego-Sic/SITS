package sits.replay.commands;

import org.junit.jupiter.api.Test;
import sits.replay.GameSummary;
import sits.replay.MoveEntry;
import sits.replay.ReplayState;
import sits.replay.TournamentSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MoveCommandTest {

    private MoveCommand move(int round, String p1, String p2, String a1, String a2, int pay1, int pay2) {
        return new MoveCommand(round, p1, p2, a1, a2, pay1, pay2);
    }

    private void assertStateIdentical(ReplayState state,
            int round, Map<String, Integer> scores, List<MoveEntry> log,
            List<GameSummary> games, Optional<TournamentSummary> finalResult) {
        assertThat(state.getCurrentRound()).isEqualTo(round);
        assertThat(state.getScores()).isEqualTo(scores);
        assertThat(state.getMoveLog()).isEqualTo(log);
        assertThat(state.getCompletedGames()).isEqualTo(games);
        assertThat(state.getFinalResult()).isEqualTo(finalResult);
    }

    @Test
    void applyAddsMoveEntryToLog() {
        ReplayState state = new ReplayState();
        move(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5).apply(state);
        assertThat(state.getMoveLog()).containsExactly(
                new MoveEntry(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5));
    }

    @Test
    void applyMergesPayoffsIntoScores() {
        ReplayState state = new ReplayState();
        move(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5).apply(state);
        assertThat(state.getScores()).containsEntry("Alice", 0);
        assertThat(state.getScores()).containsEntry("Bob", 5);
    }

    @Test
    void applyAccumulatesScoresAcrossMultipleMoves() {
        ReplayState state = new ReplayState();
        move(1, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3).apply(state);
        move(2, "Alice", "Bob", "DEFECT", "COOPERATE", 5, 0).apply(state);
        assertThat(state.getScores()).containsEntry("Alice", 8);
        assertThat(state.getScores()).containsEntry("Bob", 3);
    }

    @Test
    void applySetsCurrentRound() {
        ReplayState state = new ReplayState();
        move(7, "Alice", "Bob", "DEFECT", "DEFECT", 1, 1).apply(state);
        assertThat(state.getCurrentRound()).isEqualTo(7);
    }

    @Test
    void undoRestoresEmptyStateFromSingleMove() {
        ReplayState state = new ReplayState();
        int round = state.getCurrentRound();
        Map<String, Integer> scores = new HashMap<>(state.getScores());
        List<MoveEntry> log = new ArrayList<>(state.getMoveLog());
        List<GameSummary> games = new ArrayList<>(state.getCompletedGames());
        Optional<TournamentSummary> finalResult = state.getFinalResult();

        MoveCommand cmd = move(1, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3);
        cmd.apply(state);
        cmd.undo(state);

        assertStateIdentical(state, round, scores, log, games, finalResult);
    }

    @Test
    void undoRestoresCurrentRoundFromPreviousEntry() {
        ReplayState state = new ReplayState();
        MoveCommand first = move(1, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3);
        MoveCommand second = move(2, "Alice", "Bob", "DEFECT", "COOPERATE", 5, 0);
        first.apply(state);
        second.apply(state);
        second.undo(state);
        assertThat(state.getCurrentRound()).isEqualTo(1);
    }

    @Test
    void undoRestoresRoundToZeroWhenLogIsEmpty() {
        ReplayState state = new ReplayState();
        MoveCommand cmd = move(3, "Alice", "Bob", "DEFECT", "DEFECT", 1, 1);
        cmd.apply(state);
        cmd.undo(state);
        assertThat(state.getCurrentRound()).isZero();
    }

    @Test
    void undoRemovesParticipantFromScoresWhenBackToZero() {
        ReplayState state = new ReplayState();
        MoveCommand cmd = move(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5);
        cmd.apply(state);
        cmd.undo(state);
        assertThat(state.getScores()).doesNotContainKey("Alice");
        assertThat(state.getScores()).doesNotContainKey("Bob");
    }

    @Test
    void applyThenUndoIsIdenticalWithNonEmptyStartingState() {
        ReplayState state = new ReplayState();
        move(1, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3).apply(state);

        int round = state.getCurrentRound();
        Map<String, Integer> scores = new HashMap<>(state.getScores());
        List<MoveEntry> log = new ArrayList<>(state.getMoveLog());
        List<GameSummary> games = new ArrayList<>(state.getCompletedGames());
        Optional<TournamentSummary> finalResult = state.getFinalResult();

        MoveCommand second = move(2, "Alice", "Bob", "DEFECT", "COOPERATE", 5, 0);
        second.apply(state);
        second.undo(state);

        assertStateIdentical(state, round, scores, log, games, finalResult);
    }
}
