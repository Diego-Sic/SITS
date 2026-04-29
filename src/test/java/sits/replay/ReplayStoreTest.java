package sits.replay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

class ReplayStoreTest {

    private static final List<ReplayCommand> COMMANDS = List.of(
            new MoveCommand(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5),
            new MoveCommand(2, "Alice", "Bob", "DEFECT", "DEFECT", 1, 1),
            new GameEndCommand(new GameSummary("Bob", "Alice", "Bob", 1, 6)),
            new TournamentEndCommand(new TournamentSummary(Map.of("Alice", 1, "Bob", 6))));

    @Test
    void saveWritesReplayFile(@TempDir Path dir) throws IOException {
        new ReplayStore(dir).save("t-001", COMMANDS);
        assertThat(dir.resolve("t-001.replay")).exists();
    }

    @Test
    void loadRoundTripsAllCommandTypes(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        store.save("t-001", COMMANDS);

        ReplayFile loaded = store.load(dir.resolve("t-001.replay"));

        assertThat(loaded.getCommands()).isEqualTo(COMMANDS);
    }

    @Test
    void loadRestoresCorrectMeta(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        store.save("t-001", COMMANDS);

        ReplayFile loaded = store.load(dir.resolve("t-001.replay"));

        assertThat(loaded.getMeta().tournamentId()).isEqualTo("t-001");
        assertThat(loaded.getMeta().commandCount()).isEqualTo(COMMANDS.size());
    }

    @Test
    void listReturnsMetaForAllSavedReplays(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        store.save("t-001", COMMANDS);
        store.save("t-002", List.of(COMMANDS.get(0)));

        List<ReplayFile.Meta> metas = store.list();

        assertThat(metas).extracting(ReplayFile.Meta::tournamentId)
                .containsExactlyInAnyOrder("t-001", "t-002");
    }

    @Test
    void listReturnsCorrectCommandCount(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        store.save("t-001", COMMANDS);

        ReplayFile.Meta meta = store.list().get(0);

        assertThat(meta.commandCount()).isEqualTo(COMMANDS.size());
    }

    @Test
    void listReturnsEmptyWhenNoReplays(@TempDir Path dir) throws IOException {
        assertThat(new ReplayStore(dir).list()).isEmpty();
    }

    @Test
    void listIgnoresNonReplayFiles(@TempDir Path dir) throws IOException {
        dir.resolve("notes.txt").toFile().createNewFile();
        new ReplayStore(dir).save("t-001", COMMANDS);

        List<ReplayFile.Meta> metas = new ReplayStore(dir).list();

        assertThat(metas).hasSize(1);
    }
}
