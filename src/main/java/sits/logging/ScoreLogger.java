package sits.logging;

import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ScoreLogger implements GameObserver {

    private final String filePath;

    public ScoreLogger(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onMoveMade(MoveEvent event) {
//         I don't think we need to overwrite this function because the ScoreLogger only cares about the end
    }

    @Override
    public void onGameOver(GameResult result) {
        String p1 = result.getHistory().getNameP1();
        String p2 = result.getHistory().getNameP2();

        String line = String.format("RESULT | %s %d - %d %s | Winner: %s",
                p1, result.getTotalScoreP1(),
                result.getTotalScoreP2(), p2,
                result.getWinner());

        writeLine(line);
    }

    @Override
    public void onTournamentOver(TournamentResult result) {
        // no-op — ScoreLogger records per-game results, not tournament summaries
    }

    private void writeLine(String line) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("ScoreLogger failed to write to " + filePath + ": " + e.getMessage());
        }
    }
}
