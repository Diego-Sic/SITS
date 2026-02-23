package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MoveEventTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    @Test
    void storesRound() {
        RoundResult round = new RoundResult(COOPERATE, DEFECT, 0, 5);
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round);

        MoveEvent event = new MoveEvent(round, history);

        assertThat(event.getRound()).isSameAs(round);
    }

    @Test
    void storesHistory() {
        RoundResult round = new RoundResult(COOPERATE, DEFECT, 0, 5);
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round);

        MoveEvent event = new MoveEvent(round, history);

        assertThat(event.getHistory()).isSameAs(history);
    }

    @Test
    void historyIsUpToDateAtTimeOfEvent() {
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        RoundResult r2 = new RoundResult(COOPERATE, DEFECT, 0, 5);
        history.getRounds().add(r1);
        history.getRounds().add(r2);

        MoveEvent event = new MoveEvent(r2, history);

        assertThat(event.getHistory().getRounds()).hasSize(2);
        assertThat(event.getHistory().getLastRound()).isSameAs(r2);
    }
}
