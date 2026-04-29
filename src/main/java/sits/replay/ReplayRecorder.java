package sits.replay;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import sits.core.GameHistory;
import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.RoundResult;
import sits.core.TournamentResult;
import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

public class ReplayRecorder implements GameObserver {

    private static final Logger LOG = Logger.getLogger(ReplayRecorder.class.getName());

    private final String tournamentId;
    private final ReplayStore store;
    private final List<ReplayCommand> commands = new ArrayList<>();

    public ReplayRecorder(String tournamentId, ReplayStore store) {
        this.tournamentId = tournamentId;
        this.store = store;
    }

    @Override
    public void onMoveMade(MoveEvent event) {
        GameHistory h = event.getHistory();
        RoundResult r = event.getRound();
        commands.add(new MoveCommand(
                h.getRounds().size(),
                h.getNameP1(), h.getNameP2(),
                r.getActionP1().getLabel(), r.getActionP2().getLabel(),
                r.getPayoffP1(), r.getPayoffP2()));
    }

    @Override
    public void onGameOver(GameResult result) {
        GameHistory h = result.getHistory();
        commands.add(new GameEndCommand(new GameSummary(
                result.getWinner(),
                h.getNameP1(), h.getNameP2(),
                result.getTotalScoreP1(), result.getTotalScoreP2())));
    }

    @Override
    public void onTournamentOver(TournamentResult result) {
        commands.add(new TournamentEndCommand(new TournamentSummary(result.getSummary())));
        try {
            store.save(tournamentId, commands);
        } catch (Exception e) {
            LOG.warning(() -> "Failed to save replay for tournament " + tournamentId + ": " + e.getMessage());
        }
    }
}
