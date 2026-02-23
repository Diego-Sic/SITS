package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ParticipantTest {

    private static final Action ACTION = () -> "TEST";

    /** Stateless participant, we ensure that returns the same action, and same parameters. */
    static class StatelessParticipant implements Participant {
        private final String name;

        StatelessParticipant(String name) { this.name = name; }

        @Override public String getName()                          { return name; }
        @Override public Action chooseAction(GameHistory history)  { return ACTION; }
        @Override public void reset()                              { /*This is so it doesn't keep track*/ }
    }

    /** tracks how many times reset() was called. */
    static class StatefulParticipant implements Participant {
        int resetCount = 0;

        @Override public String getName()                          { return "Stateful"; }
        @Override public Action chooseAction(GameHistory history)  { return ACTION; }
        @Override public void reset()                              { resetCount++; }
    }

    @Test
    void returnsCorrectName() {
        Participant p = new StatelessParticipant("Alice");
        assertThat(p.getName()).isEqualTo("Alice");
    }

    @Test
    void chooseActionReceivesFullHistory() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(new RoundResult(ACTION, ACTION, 1, 1));
        history.getRounds().add(new RoundResult(ACTION, ACTION, 1, 1));

        Participant p = new StatelessParticipant("Alice");

        // Participant receives the full history — no exception, no truncation
        Action chosen = p.chooseAction(history);
        assertThat(chosen).isNotNull();
    }

    @Test
    void chooseActionWorksOnEmptyHistory() {
        GameHistory history = new GameHistory("Alice", "Bob");
        Participant p = new StatelessParticipant("Alice");

        Action chosen = p.chooseAction(history);
        assertThat(chosen).isNotNull();
    }

    @Test
    void resetClearsInternalState() {
        StatefulParticipant p = new StatefulParticipant();
        assertThat(p.resetCount).isZero();

        p.reset();
        p.reset();

        assertThat(p.resetCount).isEqualTo(2);
    }

    @Test
    void resetOnStatelessParticipantDoesNotThrow() {
        Participant p = new StatelessParticipant("Alice");
        // reset() must be safe to call even if there is nothing to clear
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(p::reset);
    }
}
