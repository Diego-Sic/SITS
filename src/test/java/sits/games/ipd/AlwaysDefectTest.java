package sits.games.ipd;

import org.junit.jupiter.api.Test;
import sits.core.GameHistory;
import sits.core.RoundResult;

import static org.assertj.core.api.Assertions.assertThat;

class AlwaysDefectTest {

    private GameHistory emptyHistory() {
        return new GameHistory("Alice", "Bob");
    }

    @Test
    void alwaysReturnsDefect() {
        AlwaysDefect participant = new AlwaysDefect();
        assertThat(participant.chooseAction(emptyHistory())).isEqualTo(PrisonerAction.DEFECT);
    }

    @Test
    void defectsOnFirstRound() {
        AlwaysDefect participant = new AlwaysDefect();
        assertThat(participant.chooseAction(emptyHistory())).isEqualTo(PrisonerAction.DEFECT);
    }

    @Test
    void defectsEvenAfterOpponentCooperates() {
        AlwaysDefect participant = new AlwaysDefect();
        GameHistory history = emptyHistory();
        history.getRounds().add(new RoundResult(
                PrisonerAction.DEFECT, PrisonerAction.COOPERATE, 5, 0));

        assertThat(participant.chooseAction(history)).isEqualTo(PrisonerAction.DEFECT);
    }

    @Test
    void nameIsAlwaysDefect() {
        assertThat(new AlwaysDefect().getName()).isEqualTo("AlwaysDefect");
    }

    @Test
    void resetDoesNotThrow() {
        AlwaysDefect participant = new AlwaysDefect();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(participant::reset);
    }
}
