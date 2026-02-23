package sits.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sits.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoveLoggerTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    @Test
    void writesRoundToFile(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("moves.log");
        MoveLogger logger = new MoveLogger(logFile.toString());

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(COOPERATE, DEFECT, 0, 5);
        history.getRounds().add(round);

        logger.onMoveMade(new MoveEvent(round, history));

        String content = Files.readString(logFile);
        assertThat(content).contains("Alice", "Bob", "COOPERATE", "DEFECT");
    }

    @Test
    void includesRoundNumber(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("moves.log");
        MoveLogger logger = new MoveLogger(logFile.toString());

        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        RoundResult r2 = new RoundResult(DEFECT, DEFECT, 1, 1);
        history.getRounds().add(r1);
        history.getRounds().add(r2);

        logger.onMoveMade(new MoveEvent(r1, history));
        logger.onMoveMade(new MoveEvent(r2, history));

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines.get(0)).contains("Round 2");
        assertThat(lines.get(1)).contains("Round 2");
    }

    @Test
    void usesGetLabelNotEnumName(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("moves.log");
        MoveLogger logger = new MoveLogger(logFile.toString());

        Action custom = () -> "MY_ACTION";
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(custom, custom, 0, 0);
        history.getRounds().add(round);

        logger.onMoveMade(new MoveEvent(round, history));

        assertThat(Files.readString(logFile)).contains("MY_ACTION");
    }

    @Test
    void appendsAcrossMultipleGames(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("moves.log");
        MoveLogger logger = new MoveLogger(logFile.toString());

        GameHistory h1 = new GameHistory("Alice", "Bob");
        RoundResult r1 = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        h1.getRounds().add(r1);

        GameHistory h2 = new GameHistory("Alice", "Carol");
        RoundResult r2 = new RoundResult(DEFECT, DEFECT, 1, 1);
        h2.getRounds().add(r2);

        logger.onMoveMade(new MoveEvent(r1, h1));
        logger.onGameOver(new GameResult(h1));
        logger.onMoveMade(new MoveEvent(r2, h2));

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).isEqualTo("---");
    }

    @Test
    void onGameOverWritesSeparator(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("moves.log");
        MoveLogger logger = new MoveLogger(logFile.toString());

        GameHistory history = new GameHistory("Alice", "Bob");
        logger.onGameOver(new GameResult(history));

        assertThat(Files.readString(logFile).trim()).isEqualTo("---");
    }
}
