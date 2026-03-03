package sits.games.ipd;

import org.junit.jupiter.api.Test;
import sits.core.GameResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IteratedPrisonersDilemmaTest {

    @Test
    void playsExactNumberOfRounds() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(5);
        GameResult result = game.play(new AlwaysCooperate(), new AlwaysCooperate());

        assertThat(result.getHistory().getRounds()).hasSize(5);
    }

    @Test
    void mutualCooperationPayoff() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);
        GameResult result = game.play(new AlwaysCooperate(), new AlwaysCooperate());

        assertThat(result.getTotalScoreP1()).isEqualTo(3);
        assertThat(result.getTotalScoreP2()).isEqualTo(3);
    }

    @Test
    void mutualDefectionPayoff() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);
        GameResult result = game.play(new AlwaysDefect(), new AlwaysDefect());

        assertThat(result.getTotalScoreP1()).isEqualTo(1);
        assertThat(result.getTotalScoreP2()).isEqualTo(1);
    }

    @Test
    void defectorExploitsCooperator() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);
        GameResult result = game.play(new AlwaysDefect(), new AlwaysCooperate());

        assertThat(result.getTotalScoreP1()).isEqualTo(5);
        assertThat(result.getTotalScoreP2()).isEqualTo(0);
    }

    @Test
    void cooperatorExploitedByDefector() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);
        GameResult result = game.play(new AlwaysCooperate(), new AlwaysDefect());

        assertThat(result.getTotalScoreP1()).isEqualTo(0);
        assertThat(result.getTotalScoreP2()).isEqualTo(5);
    }

    @Test
    void scoresAccumulateAcrossRounds() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(3);
        GameResult result = game.play(new AlwaysCooperate(), new AlwaysCooperate());

        assertThat(result.getTotalScoreP1()).isEqualTo(9);
        assertThat(result.getTotalScoreP2()).isEqualTo(9);
    }

    @Test
    void titForTatVsAlwaysCooperate() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(5);
        GameResult result = game.play(new TitForTat(), new AlwaysCooperate());

        // Both cooperate every round so it's a draw
        assertThat(result.getWinner()).isEqualTo("DRAW");
    }

    @Test
    void titForTatVsAlwaysDefect() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(5);
        GameResult result = game.play(new TitForTat(), new AlwaysDefect());

        // TFT cooperates first (oops), then mirrors defects — AlwaysDefect still wins :0
        assertThat(result.getWinner()).isEqualTo("AlwaysDefect");
    }

    @Test
    void illegalActionTypeThrows() {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);

        sits.core.Participant wrongAction = new sits.core.Participant() {
            @Override public String getName() { return "Bad"; }
            @Override public sits.core.Action chooseAction(sits.core.GameHistory h) { return () -> "UNKNOWN"; }
            @Override public void reset() {}
        };

        assertThatThrownBy(() -> game.play(wrongAction, new AlwaysCooperate()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
