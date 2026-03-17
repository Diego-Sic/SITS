package sits.participants;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;

import sits.core.Action;
import sits.core.GameHistory;
import sits.networking.StringAction;

class HumanParticipantTest {


    // This is so that we simulate user input, we just feed it a ByteArrayInputStream instead of System.in
    private HumanParticipant participantWith(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new HumanParticipant("Alice", in);
    }

    @Test
    void getName_returnsName() {
        HumanParticipant p = participantWith("C\n");
        assertEquals("Alice", p.getName());
    }

    @Test
    void chooseAction_returnsStringActionFromInput() {
        HumanParticipant p = participantWith("COOPERATE\n");
        GameHistory history = new GameHistory("Alice", "Bob");
        Action action = p.chooseAction(history);
        assertEquals("COOPERATE", action.getLabel());
    }

    @Test
    void chooseAction_trimsWhitespace() {
        HumanParticipant p = participantWith("  DEFECT  \n");
        GameHistory history = new GameHistory("Alice", "Bob");
        Action action = p.chooseAction(history);
        assertEquals("DEFECT", action.getLabel());
    }

    @Test
    void chooseAction_returnsStringAction() {
        HumanParticipant p = participantWith("COOPERATE\n");
        GameHistory history = new GameHistory("Alice", "Bob");
        Action action = p.chooseAction(history);
        assertInstanceOf(StringAction.class, action);
    }

    @Test
    void reset_doesNotThrow() {
        HumanParticipant p = participantWith("COOPERATE\n");
        assertDoesNotThrow(p::reset);
    }
}
