package sits.replay.commands;

import sits.replay.ReplayCommand;
import sits.replay.ReplayState;
import sits.replay.TournamentSummary;

import java.util.Optional;

public class TournamentEndCommand implements ReplayCommand {

    private final TournamentSummary summary;

    public TournamentEndCommand(TournamentSummary summary) {
        this.summary = summary;
    }

    @Override
    public void apply(ReplayState state) {
        state.setFinalResult(Optional.of(summary));
    }

    @Override
    public void undo(ReplayState state) {
        state.setFinalResult(Optional.empty());
    }
}
