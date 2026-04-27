package sits.replay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

class CommandTypeRegistryTest {

    @Test
    void moveReturnsCorrectClass() {
        assertThat(CommandTypeRegistry.get("MOVE")).isEqualTo(MoveCommand.class);
    }

    @Test
    void gameEndReturnsCorrectClass() {
        assertThat(CommandTypeRegistry.get("GAME_END")).isEqualTo(GameEndCommand.class);
    }

    @Test
    void tournamentEndReturnsCorrectClass() {
        assertThat(CommandTypeRegistry.get("TOURNAMENT_END")).isEqualTo(TournamentEndCommand.class);
    }

    // If you ever add a new command type, add a test here to verify it returns the
    // correct class.

    @Test
    void unknownTypeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> CommandTypeRegistry.get("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void nullTypeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> CommandTypeRegistry.get(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
