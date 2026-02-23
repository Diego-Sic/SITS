package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GameResultTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    private GameHistory historyWith(RoundResult... rounds) {
        GameHistory history = new GameHistory("Alice", "Bob");
        for (RoundResult r : rounds) {
            history.getRounds().add(r);
        }
        return history;
    }

    @Test
    void p1WinsWhenHigherScore() {
        GameHistory history = historyWith(
            new RoundResult(DEFECT, COOPERATE, 5, 0),
            new RoundResult(DEFECT, COOPERATE, 5, 0)
        );
        GameResult result = new GameResult(history);
        assertThat(result.getWinner()).isEqualTo("Alice");
    }

    @Test
    void p2WinsWhenHigherScore() {
        GameHistory history = historyWith(
            new RoundResult(COOPERATE, DEFECT, 0, 5),
            new RoundResult(COOPERATE, DEFECT, 0, 5)
        );
        GameResult result = new GameResult(history);
        assertThat(result.getWinner()).isEqualTo("Bob");
    }

    @Test
    void drawWhenEqualScores() {
        GameHistory history = historyWith(
            new RoundResult(COOPERATE, COOPERATE, 3, 3),
            new RoundResult(COOPERATE, COOPERATE, 3, 3)
        );
        GameResult result = new GameResult(history);
        assertThat(result.getWinner()).isEqualTo("DRAW");
    }

    @Test
    void totalScoresAreSummedAcrossAllRounds() {
        GameHistory history = historyWith(
            new RoundResult(COOPERATE, COOPERATE, 3, 3),
            new RoundResult(DEFECT, COOPERATE, 5, 0),
            new RoundResult(COOPERATE, DEFECT, 0, 5)
        );
        GameResult result = new GameResult(history);
        assertThat(result.getTotalScoreP1()).isEqualTo(8);
        assertThat(result.getTotalScoreP2()).isEqualTo(8);
    }

    @Test
    void historyIsPreserved() {
        GameHistory history = historyWith(
            new RoundResult(COOPERATE, COOPERATE, 3, 3)
        );
        GameResult result = new GameResult(history);
        assertThat(result.getHistory()).isSameAs(history);
    }

    @Test
    void drawOnEmptyHistory() {
        GameHistory history = new GameHistory("Alice", "Bob");
        GameResult result = new GameResult(history);
        assertThat(result.getWinner()).isEqualTo("DRAW");
        assertThat(result.getTotalScoreP1()).isZero();
        assertThat(result.getTotalScoreP2()).isZero();
    }
}
