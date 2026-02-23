package sits.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sits.core.TournamentResult;
import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;
import sits.logging.MoveLogger;
import sits.logging.ScoreLogger;
import sits.tournament.RoundRobin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndTest {

    @Test
    void fullTournamentRunsWithoutExceptions(@TempDir Path tempDir) {
        Path moveLog  = tempDir.resolve("moves.log");
        Path scoreLog = tempDir.resolve("scores.log");

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(200);
        game.addObserver(new MoveLogger(moveLog.toString()));
        game.addObserver(new ScoreLogger(scoreLog.toString()));

        TournamentResult result = new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                game);

        assertThat(result.getResults()).hasSize(3);
    }

    @Test
    void moveLogIsCreatedWithContent(@TempDir Path tempDir) throws IOException {
        Path moveLog  = tempDir.resolve("moves.log");
        Path scoreLog = tempDir.resolve("scores.log");

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(200);
        game.addObserver(new MoveLogger(moveLog.toString()));
        game.addObserver(new ScoreLogger(scoreLog.toString()));

        new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                game);

        // 3 matches × 200 rounds + 3 separators that's according of what we formatted
        assertThat(Files.readAllLines(moveLog)).hasSize(603);
    }

    @Test
    void scoreLogRecordsOneResultPerMatch(@TempDir Path tempDir) throws IOException {
        Path moveLog  = tempDir.resolve("moves.log");
        Path scoreLog = tempDir.resolve("scores.log");

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(200);
        game.addObserver(new MoveLogger(moveLog.toString()));
        game.addObserver(new ScoreLogger(scoreLog.toString()));

        new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                game);

        // One line per match
        assertThat(Files.readAllLines(scoreLog)).hasSize(3);
    }

    @Test
    void scoreLogContainsCorrectWinners(@TempDir Path tempDir) throws IOException {
        Path moveLog  = tempDir.resolve("moves.log");
        Path scoreLog = tempDir.resolve("scores.log");

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(200);
        game.addObserver(new MoveLogger(moveLog.toString()));
        game.addObserver(new ScoreLogger(scoreLog.toString()));

        new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                game);

        String scoreContent = Files.readString(scoreLog);

        // AlwaysDefect beats AlwaysCooperate
        assertThat(scoreContent).contains("Winner: AlwaysDefect");
        // AlwaysCooperate and TitForTat draw (both cooperate every round)
        assertThat(scoreContent).contains("Winner: DRAW");
    }

    @Test
    void moveLogNeverMentionsPrisonerAction(@TempDir Path tempDir) throws IOException {
        Path moveLog  = tempDir.resolve("moves.log");

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(5);
        game.addObserver(new MoveLogger(moveLog.toString()));

        new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect()),
                game);

        // MoveLogger uses getLabel() only — the enum class name must never appear
        assertThat(Files.readString(moveLog)).doesNotContain("PrisonerAction");
    }

    @Test
    void summaryScoresAreConsistentWithGameResults(@TempDir Path tempDir) {
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(10);

        TournamentResult result = new RoundRobin().run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                game);

        int summaryTotal = result.getSummary().values().stream().mapToInt(i -> i).sum();
        int gameTotal    = result.getResults().stream()
                .mapToInt(r -> r.getTotalScoreP1() + r.getTotalScoreP2())
                .sum();

        assertThat(summaryTotal).isEqualTo(gameTotal);
    }
}
