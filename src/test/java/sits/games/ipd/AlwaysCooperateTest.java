package sits.games.ipd;

import org.junit.jupiter.api.Test;
import sits.core.GameHistory;

import static org.assertj.core.api.Assertions.assertThat;

class AlwaysCooperateTest {

    @Test
    void alwaysReturnsCoooperate() {
        AlwaysCooperate participant = new AlwaysCooperate();
        GameHistory history = new GameHistory("Alice", "Bob");

        assertThat(participant.chooseAction(history)).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void cooperatesOnFirstRound() {
        AlwaysCooperate participant = new AlwaysCooperate();
        GameHistory history = new GameHistory("Alice", "Bob");

        assertThat(participant.chooseAction(history)).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void cooperatesEvenAfterOpponentDefects() {
        AlwaysCooperate participant = new AlwaysCooperate();
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(new sits.core.RoundResult(
                PrisonerAction.COOPERATE, PrisonerAction.DEFECT, 0, 5));

        assertThat(participant.chooseAction(history)).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void nameIsAlwaysCooperate() {
        assertThat(new AlwaysCooperate().getName()).isEqualTo("AlwaysCooperate");
    }

    @Test
    void resetDoesNotThrow() {
        AlwaysCooperate participant = new AlwaysCooperate();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(participant::reset);
    }
}
