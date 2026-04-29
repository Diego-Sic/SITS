package sits.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sits.core.GameHistory;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.RoundResult;
import sits.core.TournamentResult;
import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ReplayRecorderTest {

    private static final sits.core.Action COOPERATE = () -> "COOPERATE";
    private static final sits.core.Action DEFECT    = () -> "DEFECT";

    private GameHistory historyWithRounds(String p1, String p2, RoundResult... rounds) {
        GameHistory h = new GameHistory(p1, p2);
        for (RoundResult r : rounds) h.getRounds().add(r);
        return h;
    }

    @Test
    void onMoveMadeAppendsMoveCommand(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(COOPERATE, DEFECT, 0, 5);
        history.getRounds().add(round);

        recorder.onMoveMade(new MoveEvent(round, history));
        recorder.onGameOver(new GameResult(history));
        recorder.onTournamentOver(new TournamentResult(List.of(new GameResult(history))));

        ReplayFile saved = store.load(dir.resolve("t-001.replay"));
        assertThat(saved.getCommands().get(0))
                .isEqualTo(new MoveCommand(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5));
    }

    @Test
    void roundNumberMatchesHistorySizeAtTimeOfEvent(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        RoundResult r2 = new RoundResult(DEFECT, COOPERATE, 5, 0);
        history.getRounds().add(r1);
        recorder.onMoveMade(new MoveEvent(r1, history));
        history.getRounds().add(r2);
        recorder.onMoveMade(new MoveEvent(r2, history));

        recorder.onGameOver(new GameResult(history));
        recorder.onTournamentOver(new TournamentResult(List.of(new GameResult(history))));

        List<ReplayCommand> commands = store.load(dir.resolve("t-001.replay")).getCommands();
        assertThat(((MoveCommand) commands.get(0)).roundNumber()).isEqualTo(1);
        assertThat(((MoveCommand) commands.get(1)).roundNumber()).isEqualTo(2);
    }

    @Test
    void onGameOverAppendsGameEndCommand(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = historyWithRounds("Alice", "Bob",
                new RoundResult(DEFECT, COOPERATE, 5, 0));
        GameResult result = new GameResult(history);

        recorder.onGameOver(result);
        recorder.onTournamentOver(new TournamentResult(List.of(result)));

        List<ReplayCommand> commands = store.load(dir.resolve("t-001.replay")).getCommands();
        assertThat(commands.get(0))
                .isEqualTo(new GameEndCommand(new GameSummary("Alice", "Alice", "Bob", 5, 0)));
    }

    @Test
    void onTournamentOverAppendsTournamentEndAndSaves(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = historyWithRounds("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));
        TournamentResult tResult = new TournamentResult(List.of(new GameResult(history)));

        recorder.onTournamentOver(tResult);

        ReplayFile saved = store.load(dir.resolve("t-001.replay"));
        List<ReplayCommand> commands = saved.getCommands();
        assertThat(commands.get(commands.size() - 1))
                .isEqualTo(new TournamentEndCommand(
                        new TournamentSummary(Map.of("Alice", 3, "Bob", 3))));
    }

    @Test
    void fullSequenceProducesCorrectCommandList(@TempDir Path dir) throws IOException {
        ReplayStore store = new ReplayStore(dir);
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, DEFECT, 0, 5);
        RoundResult r2 = new RoundResult(COOPERATE, COOPERATE, 3, 3);

        history.getRounds().add(r1);
        recorder.onMoveMade(new MoveEvent(r1, history));
        history.getRounds().add(r2);
        recorder.onMoveMade(new MoveEvent(r2, history));

        GameResult gameResult = new GameResult(history);
        recorder.onGameOver(gameResult);

        TournamentResult tResult = new TournamentResult(List.of(gameResult));
        recorder.onTournamentOver(tResult);

        List<ReplayCommand> commands = store.load(dir.resolve("t-001.replay")).getCommands();
        assertThat(commands).isEqualTo(List.of(
                new MoveCommand(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5),
                new MoveCommand(2, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3),
                new GameEndCommand(new GameSummary("Bob", "Alice", "Bob", 3, 8)),
                new TournamentEndCommand(new TournamentSummary(Map.of("Alice", 3, "Bob", 8)))
        ));
    }

    @Test
    void failedSaveDoesNotPropagateException() {
        // point the store at a file path (not a directory) so save() will fail
        ReplayStore store = new ReplayStore(Path.of("/dev/null/impossible"));
        ReplayRecorder recorder = new ReplayRecorder("t-001", store);

        GameHistory history = historyWithRounds("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));
        TournamentResult tResult = new TournamentResult(List.of(new GameResult(history)));

        assertThatCode(() -> recorder.onTournamentOver(tResult)).doesNotThrowAnyException();
    }
}
