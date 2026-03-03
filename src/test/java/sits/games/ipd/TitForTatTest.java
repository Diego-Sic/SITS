package sits.games.ipd;

import org.junit.jupiter.api.Test;
import sits.core.GameHistory;
import sits.core.RoundResult;

import static org.assertj.core.api.Assertions.assertThat;

class TitForTatTest {

    private GameHistory emptyHistory() {
        return new GameHistory("Alice", "Bob");
    }

    private GameHistory historyWith(PrisonerAction p2Action) {
        GameHistory history = emptyHistory();
        history.getRounds().add(new RoundResult(PrisonerAction.COOPERATE, p2Action, 0, 0));
        return history;
    }

    @Test
    void cooperatesOnFirstRound() {
        TitForTat tft = new TitForTat();
        assertThat(tft.chooseAction(emptyHistory())).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void mirrorsOpponentCooperate() {
        TitForTat tft = new TitForTat();
        GameHistory history = historyWith(PrisonerAction.COOPERATE);

        assertThat(tft.chooseAction(history)).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void mirrorsOpponentDefect() {
        TitForTat tft = new TitForTat();
        GameHistory history = historyWith(PrisonerAction.DEFECT);

        assertThat(tft.chooseAction(history)).isEqualTo(PrisonerAction.DEFECT);
    }

    @Test
    void mirrorsLastRoundNotFirst() {
        TitForTat tft = new TitForTat();
        GameHistory history = emptyHistory();
        history.getRounds().add(new RoundResult(PrisonerAction.COOPERATE, PrisonerAction.DEFECT, 0, 5));
        history.getRounds().add(new RoundResult(PrisonerAction.DEFECT, PrisonerAction.COOPERATE, 5, 0));

        // last opponent move was COOPERATE so TFT should do the same :D
        assertThat(tft.chooseAction(history)).isEqualTo(PrisonerAction.COOPERATE);
    }

    @Test
    void nameIsTitForTat() {
        assertThat(new TitForTat().getName()).isEqualTo("TitForTat");
    }

    @Test
    void resetDoesNotThrow() {
        TitForTat tft = new TitForTat();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(tft::reset);
    }
}
