package sits.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class GameObserverTest {

    private static final Action COOPERATE = () -> "COOPERATE";

    /**
     * Test-only observer that records every event it receives.
     */
    static class CapturingObserver implements GameObserver {
        List<MoveEvent> moveEvents = new ArrayList<>();
        List<GameResult> gameResults = new ArrayList<>();
        List<TournamentResult> tournamentResults = new ArrayList<>();

        @Override
        public void onMoveMade(MoveEvent event) { moveEvents.add(event); }

        @Override
        public void onGameOver(GameResult result) { gameResults.add(result); }

        @Override
        public void onTournamentOver(TournamentResult result) { tournamentResults.add(result); }
    }

    @Test
    void onMoveMadeReceivesMoveEvent() {
        CapturingObserver observer = new CapturingObserver();
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        history.getRounds().add(round);

        observer.onMoveMade(new MoveEvent(round, history));

        assertThat(observer.moveEvents).hasSize(1);
        assertThat(observer.moveEvents.get(0).getRound()).isSameAs(round);
    }

    @Test
    void onGameOverReceivesGameResult() {
        CapturingObserver observer = new CapturingObserver();
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(new RoundResult(COOPERATE, COOPERATE, 3, 3));
        GameResult result = new GameResult(history);

        observer.onGameOver(result);

        assertThat(observer.gameResults).hasSize(1);
        assertThat(observer.gameResults.get(0)).isSameAs(result);
    }

    @Test
    void moveAndGameEventsAreIndependent() {
        CapturingObserver observer = new CapturingObserver();
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        history.getRounds().add(round);

        observer.onMoveMade(new MoveEvent(round, history));
        observer.onMoveMade(new MoveEvent(round, history));
        observer.onGameOver(new GameResult(history));

        assertThat(observer.moveEvents).hasSize(2);
        assertThat(observer.gameResults).hasSize(1);
    }
}
