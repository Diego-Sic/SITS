package sits.replay;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ReplayStateTest {

    @Test
    void startsAtRoundZero() {
        ReplayState state = new ReplayState();
        assertThat(state.getCurrentRound()).isZero();
    }

    @Test
    void startsWithEmptyScores() {
        ReplayState state = new ReplayState();
        assertThat(state.getScores()).isEmpty();
    }

    @Test
    void startsWithEmptyMoveLog() {
        ReplayState state = new ReplayState();
        assertThat(state.getMoveLog()).isEmpty();
    }

    @Test
    void startsWithNoCompletedGames() {
        ReplayState state = new ReplayState();
        assertThat(state.getCompletedGames()).isEmpty();
    }

    @Test
    void startsWithNoFinalResult() {
        ReplayState state = new ReplayState();
        assertThat(state.getFinalResult()).isEmpty();
    }

    @Test
    void currentRoundIsMutable() {
        ReplayState state = new ReplayState();
        state.setCurrentRound(5);
        assertThat(state.getCurrentRound()).isEqualTo(5);
    }

    @Test
    void scoresMergesPayoffForNewParticipant() {
        ReplayState state = new ReplayState();
        state.getScores().put("Alice", 3);
        assertThat(state.getScores()).containsEntry("Alice", 3);
    }

    @Test
    void moveLogAcceptsEntries() {
        ReplayState state = new ReplayState();
        MoveEntry entry = new MoveEntry(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5);
        state.getMoveLog().add(entry);
        assertThat(state.getMoveLog()).containsExactly(entry);
    }

    @Test
    void completedGamesAcceptsEntries() {
        ReplayState state = new ReplayState();
        GameSummary summary = new GameSummary("Alice", "Alice", "Bob", 9, 6);
        state.getCompletedGames().add(summary);
        assertThat(state.getCompletedGames()).containsExactly(summary);
    }

    @Test
    void finalResultIsMutable() {
        ReplayState state = new ReplayState();
        TournamentSummary summary = new TournamentSummary(Map.of("Alice", 9, "Bob", 6));
        state.setFinalResult(Optional.of(summary));
        assertThat(state.getFinalResult()).contains(summary);
    }

    @Test
    void finalResultCanBeCleared() {
        ReplayState state = new ReplayState();
        TournamentSummary summary = new TournamentSummary(Map.of("Alice", 9));
        state.setFinalResult(Optional.of(summary));
        state.setFinalResult(Optional.empty());
        assertThat(state.getFinalResult()).isEmpty();
    }

    @Test
    void moveEntryFieldsAreAccessible() {
        MoveEntry entry = new MoveEntry(3, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3);
        assertThat(entry.roundNumber()).isEqualTo(3);
        assertThat(entry.nameP1()).isEqualTo("Alice");
        assertThat(entry.nameP2()).isEqualTo("Bob");
        assertThat(entry.actionP1()).isEqualTo("COOPERATE");
        assertThat(entry.actionP2()).isEqualTo("COOPERATE");
        assertThat(entry.payoffP1()).isEqualTo(3);
        assertThat(entry.payoffP2()).isEqualTo(3);
    }

    @Test
    void gameSummaryFieldsAreAccessible() {
        GameSummary summary = new GameSummary("Alice", "Alice", "Bob", 9, 6);
        assertThat(summary.winner()).isEqualTo("Alice");
        assertThat(summary.nameP1()).isEqualTo("Alice");
        assertThat(summary.nameP2()).isEqualTo("Bob");
        assertThat(summary.scoreP1()).isEqualTo(9);
        assertThat(summary.scoreP2()).isEqualTo(6);
    }

    @Test
    void tournamentSummaryFieldsAreAccessible() {
        Map<String, Integer> scores = Map.of("Alice", 9, "Bob", 6);
        TournamentSummary summary = new TournamentSummary(scores);
        assertThat(summary.finalScores()).containsEntry("Alice", 9);
        assertThat(summary.finalScores()).containsEntry("Bob", 6);
    }
}
