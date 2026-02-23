package sits.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    private static final Action ACTION = () -> "TEST";

    /** Plays exactly maxRounds rounds, always returning a fixed RoundResult. */
    static class StubGame extends Game {
        private final int maxRounds;

        StubGame(int maxRounds) { this.maxRounds = maxRounds; }

        @Override
        protected RoundResult doRound(Participant p1, Participant p2, GameHistory history) {
            return new RoundResult(ACTION, ACTION, 1, 1);
        }

        @Override
        protected boolean isOver(GameHistory history) {
            return history.getRounds().size() >= maxRounds;
        }

        @Override
        protected GameResult computeFinalResult(GameHistory history) {
            return new GameResult(history);
        }
    }

    static class StubParticipant implements Participant {
        private final String name;
        int resetCount = 0;

        StubParticipant(String name) { this.name = name; }

        @Override public String getName() { return name; }
        @Override public Action chooseAction(GameHistory history) { return ACTION; }
        @Override public void reset() { resetCount++; }
    }

    static class CapturingObserver implements GameObserver {
        List<MoveEvent> moveEvents = new ArrayList<>();
        List<GameResult> gameResults = new ArrayList<>();
        List<Integer> historySizesAtNotification = new ArrayList<>();

        @Override
        public void onMoveMade(MoveEvent event) {
            moveEvents.add(event);
            // Record size NOW, while the notification is live — not post-hoc
            historySizesAtNotification.add(event.getHistory().getRounds().size());
        }

        @Override public void onGameOver(GameResult result) { gameResults.add(result); }
    }

    // --- Tests ---

    private StubParticipant p1;
    private StubParticipant p2;

    @BeforeEach
    void setUp() {
        p1 = new StubParticipant("Alice");
        p2 = new StubParticipant("Bob");
    }

    @Test
    void playRunsCorrectNumberOfRounds() {
        Game game = new StubGame(5);
        GameResult result = game.play(p1, p2);

        assertThat(result.getHistory().getRounds()).hasSize(5);
    }

    @Test
    void playReturnsGameResultWithCorrectPlayerNames() {
        Game game = new StubGame(3);
        GameResult result = game.play(p1, p2);

        assertThat(result.getHistory().getNameP1()).isEqualTo("Alice");
        assertThat(result.getHistory().getNameP2()).isEqualTo("Bob");
    }

    @Test
    void observerReceivesOneMoveEventPerRound() {
        Game game = new StubGame(4);
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        game.play(p1, p2);

        assertThat(observer.moveEvents).hasSize(4);
    }

    @Test
    void observerReceivesGameOverExactlyOnce() {
        Game game = new StubGame(3);
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        game.play(p1, p2);

        assertThat(observer.gameResults).hasSize(1);
    }

    @Test
    void moveEventCarriesUpToDateHistory() {
        Game game = new StubGame(3);
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        game.play(p1, p2);

        assertThat(observer.historySizesAtNotification).containsExactly(1, 2, 3);
    }

    @Test
    void multipleObserversAllReceiveEvents() {
        Game game = new StubGame(2);
        CapturingObserver obs1 = new CapturingObserver();
        CapturingObserver obs2 = new CapturingObserver();
        game.addObserver(obs1);
        game.addObserver(obs2);

        game.play(p1, p2);

        assertThat(obs1.moveEvents).hasSize(2);
        assertThat(obs2.moveEvents).hasSize(2);
    }

    @Test
    void removedObserverStopsReceivingEvents() {
        Game game = new StubGame(3);
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);
        game.removeObserver(observer);

        game.play(p1, p2);

        assertThat(observer.moveEvents).isEmpty();
        assertThat(observer.gameResults).isEmpty();
    }

    @Test
    void zeroRoundGameFiresGameOverImmediately() {
        Game game = new StubGame(0);
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        game.play(p1, p2);

        assertThat(observer.moveEvents).isEmpty();
        assertThat(observer.gameResults).hasSize(1);
    }
}
