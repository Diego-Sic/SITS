package sits.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sits.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreLoggerTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    private GameResult gameResult(String p1, String p2, RoundResult... rounds) {
        GameHistory history = new GameHistory(p1, p2);
        for (RoundResult r : rounds) history.getRounds().add(r);
        return new GameResult(history);
    }

    @Test
    void writesResultOnGameOver(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("scores.log");
        ScoreLogger logger = new ScoreLogger(logFile.toString());

        GameResult result = gameResult("Alice", "Bob",
                new RoundResult(DEFECT, COOPERATE, 5, 0));

        logger.onGameOver(result);

        String content = Files.readString(logFile);
        assertThat(content).contains("Alice", "Bob", "5", "0");
    }

    @Test
    void includesWinnerInOutput(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("scores.log");
        ScoreLogger logger = new ScoreLogger(logFile.toString());

        GameResult result = gameResult("Alice", "Bob",
                new RoundResult(DEFECT, COOPERATE, 5, 0));

        logger.onGameOver(result);

        assertThat(Files.readString(logFile)).contains("Alice");
    }

    @Test
    void includesDrawInOutput(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("scores.log");
        ScoreLogger logger = new ScoreLogger(logFile.toString());

        GameResult result = gameResult("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));

        logger.onGameOver(result);

        assertThat(Files.readString(logFile)).contains("DRAW");
    }

    @Test
    void onMoveMadeDoesNotWriteToFile(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("scores.log");
        ScoreLogger logger = new ScoreLogger(logFile.toString());

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        history.getRounds().add(round);

        logger.onMoveMade(new MoveEvent(round, history));

        assertThat(logFile).doesNotExist();
    }

    @Test
    void appendsAcrossMultipleGames(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("scores.log");
        ScoreLogger logger = new ScoreLogger(logFile.toString());

        GameResult r1 = gameResult("Alice", "Bob",
                new RoundResult(DEFECT, COOPERATE, 5, 0));
        GameResult r2 = gameResult("Alice", "Carol",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));

        logger.onGameOver(r1);
        logger.onGameOver(r2);

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).contains("Bob");
        assertThat(lines.get(1)).contains("Carol");
    }
}
