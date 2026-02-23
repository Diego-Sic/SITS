package sits.logging;

import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.RoundResult;
import sits.core.TournamentResult;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MoveLogger implements GameObserver {

    private final String filePath;

    public MoveLogger(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onMoveMade(MoveEvent event) {
        RoundResult round = event.getRound();
        String p1 = event.getHistory().getNameP1();
        String p2 = event.getHistory().getNameP2();
        int roundNumber = event.getHistory().getRounds().size();

        String line = String.format("Round %d | %s: %s (%d pts) | %s: %s (%d pts)",
                roundNumber,
                p1, round.getActionP1().getLabel(), round.getPayoffP1(),
                p2, round.getActionP2().getLabel(), round.getPayoffP2());

        writeLine(line);
    }

    @Override
    public void onGameOver(GameResult result) {
        writeLine("---");
    }

    @Override
    public void onTournamentOver(TournamentResult result) {
        // no-op — MoveLogger only tracks round-by-round moves
    }

    private void writeLine(String line) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("MoveLogger failed to write to " + filePath + ": " + e.getMessage());
        }
    }
}
