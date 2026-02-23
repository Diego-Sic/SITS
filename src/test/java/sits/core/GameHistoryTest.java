package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GameHistoryTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    @Test
    void storesPlayerNames() {
        GameHistory history = new GameHistory("Alice", "Bob");
        assertThat(history.getNameP1()).isEqualTo("Alice");
        assertThat(history.getNameP2()).isEqualTo("Bob");
    }

    @Test
    void startsWithNoRounds() {
        GameHistory history = new GameHistory("Alice", "Bob");
        assertThat(history.getRounds()).isEmpty();
    }

    @Test
    void getLastRoundReturnsNullWhenEmpty() {
        GameHistory history = new GameHistory("Alice", "Bob");
        assertThat(history.getLastRound()).isNull();
    }

    @Test
    void roundsAreMutableAndAccumulateCorrectly() {
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        RoundResult r2 = new RoundResult(COOPERATE, DEFECT, 0, 5);

        history.getRounds().add(r1);
        history.getRounds().add(r2);

        assertThat(history.getRounds()).hasSize(2);
    }

    @Test
    void getLastRoundReturnsNewestRound() {
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        RoundResult r2 = new RoundResult(DEFECT, DEFECT, 1, 1);

        history.getRounds().add(r1);
        history.getRounds().add(r2);

        assertThat(history.getLastRound()).isEqualTo(r2);
    }

    @Test
    void getLastRoundDoesNotRemoveRound() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(new RoundResult(COOPERATE, COOPERATE, 3, 3));

        history.getLastRound();

        assertThat(history.getRounds()).hasSize(1);
    }
}
